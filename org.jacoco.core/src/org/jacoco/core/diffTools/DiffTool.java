/*******************************************************************************
 * Copyright (c) 2009, 2021 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/

package org.jacoco.core.diffTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.security.MessageDigest;

public class DiffTool {
	private static final String theDiffFilePath = new String(" initvalue");
	private final HashMap<String, ArrayList> classMethods = getclassMethodsMapper(
			theDiffFilePath);

	public boolean isDiffFileExists() {
		if (theDiffFilePath == null || theDiffFilePath.equals("initvalue")
				|| theDiffFilePath.equals("") || this.classMethods.isEmpty()) {
			return false;
		}
		return true;
	}

	public String getDiffFile() {
		return theDiffFilePath;
	}

	public static HashMap<String, ArrayList> getclassMethodsMapper(
			String file) {
		HashMap<String, ArrayList> diffMap = new HashMap<String, ArrayList>();
		if (file == null || file.equals("initvalue") || file.equals("")) {
			return diffMap;
		}
		String[] classArray = file.split("%");
		for (int i = 0; i < classArray.length; i++) {
			if (!classArray[i].contains(":")) {
				continue;
			}
			String[] methodInfors = classArray[i].split(":");
			String classname = methodInfors[0];
			String methodstr = methodInfors[1];
			ArrayList<String> list = new ArrayList<String>(
					Arrays.asList(methodstr.split("#")));
			diffMap.put(classname, list);
			return diffMap;
		}
		return diffMap;
	}

	public boolean isDiffMethod(String className, String methodName,
			String methodDesc) {
		for (String key : this.classMethods.keySet()) {
			if (key.endsWith(className)) {
				ArrayList<String> methodArray = this.classMethods.get(key);
				if (((String) methodArray.get(0)).equals("true")) {
					return true;
				}
				String[] paramArr = methodDesc.split("\\)")[0].split(";");
				StringBuilder builder = new StringBuilder(methodName);
				for (String param : paramArr) {
					String[] tt = param.split("/");
					if (tt.length >= 2) {
						builder.append("," + tt[tt.length - 1]);
					}
				}
				for (String method : methodArray) {
					if (method.equals(builder.toString())) {
						return true;
					}
				}
			}
		}
		return false;

	}

	public static void modify(String fieldName, Object newFieldValue)
			throws Exception {
		if (!theDiffFilePath.equals("initvalue")) {
			return;
		}
		Class<?> clazz = Class
				.forName("org.jacoco.cli.internal.core.diffTools.DiffTool");
		DiffTool diffTool = (DiffTool) clazz.newInstance();
		Field field = diffTool.getClass().getDeclaredField(fieldName);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEf);
		if (!field.isAccessible()) {
			field.setAccessible(true);
		}
		field.set(diffTool, newFieldValue);
	}

	public static String getMD5Value(String dataStr) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(dataStr.getBytes("UTF8"));
			byte[] s = messageDigest.digest();
			String result = "";
			for (int i = 0; i < s.length; i++) {
				result = result + Integer.toHexString(0xFF & s[i] | 0xFFFFFF00)
						.substring(6);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

}
