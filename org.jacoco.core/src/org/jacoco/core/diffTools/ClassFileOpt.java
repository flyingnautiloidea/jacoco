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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ClassFileOpt {
	public String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public String readStringFromFile(String filePath) {
		if (filePath == null) {
			return null;
		}
		File classesFile = new File(filePath);
		if (classesFile.isFile()) {
			ClassFileOpt fo = new ClassFileOpt();
			String includesClassesString = new String();
			try {
				includesClassesString = fo.readFile(filePath,
						StandardCharsets.UTF_8);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return includesClassesString.trim() == "" ? null
					: includesClassesString.trim();
		} else {
			return null;
		}
	}

	public String includesString(Map<String, String> options) {
		String newIncludesString = new String();
		String includesString = options.get("includes");
		String includesFilePath = options.get("includesfilepath");
		String includesFileContent = readStringFromFile(includesFilePath);
		if (includesString == null || includesString.equals("")) {
			if (includesFileContent == null) {
				return null;
			} else {
				return includesFileContent;
			}
		} else {
			if (includesFileContent == null) {
				return includesString;
			} else {
				/** both of them are hava values not equal "" */
				if (includesString.equals("*")) {
					return includesFileContent;
				} else {
					return includesString + ":" + includesFileContent;
				}
			}
		}

	}

	/** for cli diffFilex */
	String getArrayValue(String[] options, String key) {
		String value = new String();
		value = null;
		for (int inter = 0; inter < options.length; inter++) {
			String currentEle = options[inter];
			if (currentEle.equals(key)) {
				/** params signals : -- */
				if ((inter + 1 <= options.length - 1)
						&& !options[inter + 1].contains("--")) {
					value = options[inter + 1];
					break;
				}
			}
		}
		return value;
	}

	public String diffFileString(String[] options) {
		String diffArgsValue = getArrayValue(options, "--diffFile");
		String diffFileLPath = getArrayValue(options, "--diffFileL");
		String diffFileContent = readStringFromFile(diffFileLPath);
		String finalDiffFile = new String();
		finalDiffFile = null;
		if (diffFileContent == null) {
			finalDiffFile = diffArgsValue;
		} else {
			if (diffArgsValue == null || diffArgsValue.equals("")
					|| diffArgsValue.equals("*")) {
				finalDiffFile = diffFileContent;
			} else {
				finalDiffFile = diffArgsValue + "%" + diffFileContent;
			}
		}
		return finalDiffFile;
	}

	public String[] assembleFinalArgs(String[] args) {
		List<String> finalArgsList = new ArrayList<String>();
		String finalDiffFileString = diffFileString(args);
		for (int i = 0; i < args.length; i++) {
			String currentValue = args[i];
			if (currentValue.equals("--diffFile")) {
				finalArgsList.add(currentValue);
				finalArgsList.add(
						finalDiffFileString == null ? "" : finalDiffFileString);
				if (i + 1 <= args.length - 1) {
					if (!args[i + 1].contains("--")) {
						i = i + 1;
					}
				}
				continue;
			} else if (currentValue.equals("--diffFileL")) {
				if (1 + 1 < args.length - 1) {
					if (!args[i + 1].contains("--")) {
						i = i + 1;
					}
				}
				continue;
			}
			finalArgsList.add(currentValue);
		}
		int finalArgsLen = finalArgsList.size();
		String[] finalArgs = new String[finalArgsLen];
		for (int inter = 0; inter < finalArgsLen; inter++) {
			finalArgs[inter] = finalArgsList.get(inter);
		}
		return finalArgs;
	}

}
