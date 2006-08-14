/*
 * Copyright 2005 [ini4j] Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ini4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * JDK JAR Services API alap� service keres� oszt�ly.
 *
 * @author Szkiba Iv�n
 * @version $Name$
 */
class ServiceFinder {
	/**
	 * Service objektum keres�s �s p�ld�nyos�t�s
	 *
	 * a JDK JAR specifik�ci�ban defini�lt <B>Services API</B>-nak
	 * megfelel�en service oszt�ly keres�s, majd pedig p�ld�ny k�pz�s a context
	 * ClassLoader seg�ts�g�vel.</p><p>
	 * Az implement�l� oszt�ly n�v keres�se a <CODE>serviceId</CODE> nev�
	 * system property vizsg�lat�val kezd�dik. Amennyiben nincs ilyen
	 * property, �gy a keres�s a
	 * <CODE>/META-INF/services/<I>serviceId</I></CODE> nev� file tartalm�val
	 * folytat�dik. Amennyiben nincs ilyen nev� file, �gy a param�terk�nt �tadott
	 * <CODE>defaultService</CODE> lesz az oszt�ly neve.</p><p>
	 * A fenti keres�st k�vet�en t�rt�nik a p�ld�ny k�pz�s. A visszat�r�si
	 * �rt�k mindig egy val�di objektum, l�v�n minden hiba exception-t gener�l.
	 * @param serviceId keresett oszt�ly/service neve
	 * @param defaultService alap�rtelmezett implement�l� oszt�ly neve
	 * @throws IllegalArgumentException keres�si vagy p�ld�nyos�t�si hiba eset�n
	 * @return a keresett oszt�ly implement�l� objektum
	 */
	protected static Object findService(final String serviceId, final String defaultService) throws IllegalArgumentException {
		try {
			return findServiceClass(serviceId, defaultService).newInstance();
		} catch (final Exception x) {
			throw (IllegalArgumentException) new IllegalArgumentException("Provider " + serviceId + " could not be instantiated: " + x).initCause(x); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Service oszt�ly keres�s
	 *
	 * a JDK JAR specifik�ci�ban defini�lt <B>Services API</B>-nak
	 * megfelel�en service oszt�ly keres�s.</p><p>
	 * Az implement�l� oszt�ly n�v keres�se a <CODE>serviceId</CODE> nev�
	 * system property vizsg�lat�val kezd�dik. Amennyiben nincs ilyen
	 * property, �gy a keres�s a
	 * <CODE>/META-INF/services/<I>serviceId</I></CODE> nev� file tartalm�val
	 * folytat�dik. Amennyiben nincs ilyen nev� file, �gy a param�terk�nt �tadott
	 * <CODE>defaultService</CODE> lesz az oszt�ly neve.</p><p>
	 * @param serviceId keresett oszt�ly/service neve
	 * @param defaultService alap�rtelmezett implement�l� oszt�ly neve
	 * @throws IllegalArgumentException keres�si vagy p�ld�nyos�t�si hiba eset�n
	 * @return a keresett oszt�ly objektum
	 */
	protected static Class findServiceClass(final String serviceId, final String defaultService) throws IllegalArgumentException {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		final String serviceClassName = findServiceClassName(serviceId, defaultService);

		try {
			return classLoader == null ? Class.forName(serviceClassName) : classLoader.loadClass(serviceClassName);
		} catch (final ClassNotFoundException x) {
			throw (IllegalArgumentException) new IllegalArgumentException("Provider " + serviceClassName + " not found").initCause(x); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Service oszt�ly nev�nek keres�se
	 *
	 * a JDK JAR specifik�ci�ban defini�lt <B>Services API</B>-nak
	 * megfelel�en service oszt�ly keres�s.</p><p>
	 * Az implement�l� oszt�ly n�v keres�se a <CODE>serviceId</CODE> nev�
	 * system property vizsg�lat�val kezd�dik. Amennyiben nincs ilyen
	 * property, �gy a keres�s a
	 * <CODE>/META-INF/services/<I>serviceId</I></CODE> nev� file tartalm�val
	 * folytat�dik. Amennyiben nincs ilyen nev� file, �gy a param�terk�nt �tadott
	 * <CODE>defaultService</CODE> lesz az oszt�ly neve.</p><p>
	 * @param serviceId keresett oszt�ly/service neve
	 * @param defaultService alap�rtelmezett implement�l� oszt�ly neve
	 * @throws IllegalArgumentException keres�si vagy p�ld�nyos�t�si hiba eset�n
	 * @return a keresett oszt�ly neve
	 */
	protected static String findServiceClassName(final String serviceId, final String defaultService) throws IllegalArgumentException {
		if (defaultService == null)
			throw new IllegalArgumentException("Provider for " + serviceId + " cannot be found"); //$NON-NLS-1$ //$NON-NLS-2$

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String serviceClassName = null;

		// Use the system property first
		try {
			final String systemProp = System.getProperty(serviceId);

			if (systemProp != null)
				serviceClassName = systemProp;
		} catch (final SecurityException x) {
			;
		}

		if (serviceClassName == null) {
			final String servicePath = "META-INF/services/" + serviceId; //$NON-NLS-1$

			// try to find services in CLASSPATH
			try {
				InputStream is = null;

				if (classLoader == null)
					is = ClassLoader.getSystemResourceAsStream(servicePath);
				else
					is = classLoader.getResourceAsStream(servicePath);

				if (is != null) {
					final BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$

					String line = rd.readLine();
					rd.close();

					if (line != null && !"".equals(line = line.trim())) //$NON-NLS-1$
						serviceClassName = line.split("\\s|#")[0]; //$NON-NLS-1$
				}
			} catch (final Exception x) {
				;
			}
		}

		if (serviceClassName == null)
			serviceClassName = defaultService;

		return serviceClassName;
	}
}
