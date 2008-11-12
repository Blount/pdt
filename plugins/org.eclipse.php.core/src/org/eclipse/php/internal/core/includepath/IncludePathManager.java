package org.eclipse.php.internal.core.includepath;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.dltk.core.*;
import org.eclipse.php.internal.core.preferences.CorePreferencesSupport;

public class IncludePathManager {

	private static final String PREF_KEY = "include_path"; //$NON-NLS-1$
	private static final char PREF_SEP = (char) 5;

	private static IncludePathManager instance = new IncludePathManager();

	private IncludePathManager() {
		DLTKCore.addElementChangedListener(new IElementChangedListener() {

			public void elementChanged(ElementChangedEvent event) {
				processChildren(event.getDelta());
			}
			
			private void processChildren(IModelElementDelta delta) {
				IModelElement element = delta.getElement();
				try {
					if ((delta.getFlags() & IModelElementDelta.F_BUILDPATH_CHANGED) != 0) {
						IScriptProject scriptProject = element.getScriptProject();
						IProject project = scriptProject.getProject();
						IncludePath[] includePathEntries = getIncludePath(project);
						List<IncludePath> newEntries = new LinkedList<IncludePath>(Arrays.asList(includePathEntries));
						
						IBuildpathEntry[] rawBuildpath = scriptProject.getRawBuildpath();
						
						// This is a workaround to the lack of information about buildpath changes in delta:
						boolean changed = false;
						
						// Calculate added entries: 
						for (IBuildpathEntry entry : rawBuildpath) {
							boolean added = true;
							for (IncludePath includePath : includePathEntries) {
								if (includePath.isBuildpath() && entry == includePath.getEntry()) {
									added = false;
									break;
								}
							}
							if (added && isBuildpathAllowed(entry)) {
								newEntries.add(new IncludePath(entry));
								changed = true;
							}
						}
						
						// Calculate removed entries:
						List<IncludePath> entriesToRemove = new LinkedList<IncludePath>();
						for (IncludePath includePath : includePathEntries) {
							boolean removed = true;
							for (IBuildpathEntry entry : rawBuildpath) {
								if (includePath.isBuildpath()) {
									if (entry == includePath.getEntry()) {
										removed = false;
										break;
									}
								} else {
									if (entry.getPath().isPrefixOf(((IResource)includePath.getEntry()).getFullPath())) {
										removed = false;
										break;
									}
								}
							}
							if (removed) {
								entriesToRemove.add(includePath);
							}
						}
						for (IncludePath includePath : entriesToRemove) {
							newEntries.remove(includePath);
							changed = true;
						}

						if (changed) {
							setIncludePath(project, newEntries.toArray(new IncludePath[newEntries.size()]));
						}
						
					} else {
						IModelElementDelta[] children = delta.getAffectedChildren();
						for (IModelElementDelta child : children) {
							processChildren(child);
						}
					}
				} catch (ModelException e) {
					if (DLTKCore.DEBUG) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public static IncludePathManager getInstance() {
		return instance;
	}

	/**
	 * Read project's include path 
	 * @param project
	 * @return ordered include path
	 */
	public IncludePath[] getIncludePath(IProject project) {

		List<IncludePath> includePathEntries = new LinkedList<IncludePath>();

		String includePath = CorePreferencesSupport.getInstance().getProjectSpecificPreferencesValue(PREF_KEY, null, project);
		if (includePath != null) {
			while (includePath != null) {
				String path;
				int i = includePath.indexOf(PREF_SEP);
				if (i == -1) {
					path = includePath;
					includePath = null;
				} else {
					path = includePath.substring(0, i);
					includePath = includePath.substring(i + 1);
				}

				if ((i = path.indexOf(';')) != -1) {
					int kind = Integer.parseInt(path.substring(0, i));
					path = path.substring(i + 1);

					if (kind == 0) {
						IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
						if (resource != null) {
							includePathEntries.add(new IncludePath(resource));
						}
					} else {
						IBuildpathEntry[] buildpath;
						try {
							buildpath = DLTKCore.create(project).getRawBuildpath();
							for (IBuildpathEntry entry : buildpath) {
								if (entry.getEntryKind() == kind && entry.getPath().equals(new Path(path))) {
									includePathEntries.add(new IncludePath(entry));
								}
							}
						} catch (ModelException e) {
							if (DLTKCore.DEBUG) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		} else { // by default include path equals to the build path
			try {
				IBuildpathEntry[] buildpath = DLTKCore.create(project).getRawBuildpath();
				for (IBuildpathEntry entry : buildpath) {
					if (isBuildpathAllowed(entry)) {
						includePathEntries.add(new IncludePath(entry));
					}
				}
			} catch (ModelException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
		}
		return includePathEntries.toArray(new IncludePath[includePathEntries.size()]);
	}

	/**
	 * Sets project's include path
	 * @param project
	 * @param includePathEntries Ordered include path entries
	 */
	public void setIncludePath(final IProject project, IncludePath[] includePathEntries) {
		final StringBuilder buf = new StringBuilder();
		for (int i = 0; i < includePathEntries.length; ++i) {
			IncludePath includePath = includePathEntries[i];
			if (includePath.isBuildpath()) {
				IBuildpathEntry entry = (IBuildpathEntry) includePath.getEntry();
				buf.append(entry.getEntryKind()).append(';').append(entry.getPath().toString());
			} else {
				IResource entry = (IResource) includePath.getEntry();
				buf.append("0;").append(entry.getFullPath().toString());
			}
			if (i < includePathEntries.length - 1) {
				buf.append(PREF_SEP);
			}
		}
		WorkspaceJob job = new WorkspaceJob("Modifying Include Path") {
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				CorePreferencesSupport.getInstance().setProjectSpecificPreferencesValue(PREF_KEY, buf.toString(), project);
				return Status.OK_STATUS;
			}
		};
		job.setRule(project.getWorkspace().getRoot());
		job.schedule();
	}
	
	public static boolean isBuildpathAllowed(IBuildpathEntry entry) {
		return (entry.getEntryKind() == IBuildpathEntry.BPE_LIBRARY || entry.getEntryKind() == IBuildpathEntry.BPE_PROJECT); // && entry.getPath().toString().equals(LanguageModelInitializer.CONTAINER_PATH)
	}
}
