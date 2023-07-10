package org.jacoco.core.tools;
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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.data.CRC64;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import com.alibaba.fastjson.JSONObject;

public class javaByteFuncMapAfter {
	private String theKeySpliter = "-$j$k$-";

	public Map<String, String> extract(final byte[] source) {
		Map<String, String> funcHash = new HashMap<String, String>();

		return funcHash;

	}

	public Map<String, Long> genClassFuncMapFromBufferAndName(
			final byte[] buffer, final String className) {
		Map<String, Long> funcHash = new HashMap<String, Long>();

		try {
			InputStream sbs = new ByteArrayInputStream(buffer);

			final ClassParser parser = new ClassParser(sbs, className);

			final JavaClass clazz = parser.parse();

			String theClassName = clazz.getClassName();

			Method[] methods = clazz.getMethods();

			int methodSize = methods.length;

			for (int methodCount = 0; methodCount < methodSize; methodCount++) {
				Method currentMethod = methods[methodCount];

				String methodName = currentMethod.getName();
				String methodSignature = currentMethod.getSignature();
				Code code = currentMethod.getCode();
				String methodkey = keyBuilderForQuery(className, methodName,
						methodSignature);
				String codeString = (code == null ? methodkey
						: codeWithoutConstantpoolIndex(code.toString())
								+ methodkey);
				final long classId = CRC64.classId(codeString.getBytes());
				long methodValue = classId;
				funcHash.put(methodkey, methodValue);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return funcHash;
	}

	public String keyBuilderForQuery(String owner, String name, String desc) {
		return owner.replace("/", ",") + theKeySpliter + name + theKeySpliter
				+ desc;
	}

	public String codeWithoutConstantpoolIndex(String originCodeStr) {
		return originCodeStr.replaceAll("\\(\\d*\\s*\\,*\\s*\\d*\\)", "()");
	}

	public void rebuildClassProbe(File crrentFile,
			final ExecutionDataStore classData,
			final ExecutionDataStore methodData) throws Exception {
		if (crrentFile.isDirectory()) {
			for (final File f : crrentFile.listFiles()) {
				rebuildClassProbe(f, classData, methodData);
			}
		} else {
			final InputStream in = new FileInputStream(crrentFile);
			// to rebuildclass probes
			try {
				final ClassParser parser = new ClassParser(in,
						crrentFile.getName());
				final JavaClass clazz = parser.parse();
				String className = clazz.getClassName();
				Long classId = CRC64.classId(clazz.getBytes());
				boolean[] classProbe = new boolean[0];
				Method[] methods = clazz.getMethods();
				int methodSize = methods.length;
				for (int methodCount = 0; methodCount < methodSize; methodCount++) {
					Method currentMethod = methods[methodCount];
					String methodName = currentMethod.getName();
					String methodSignature = currentMethod.getSignature();
					String methodkey = keyBuilderForQuery(className, methodName,
							methodSignature);
					Code code = currentMethod.getCode();
					String codeString = (code == null ? methodkey
							: codeWithoutConstantpoolIndex(code.toString())
									+ methodkey);
					/** * to generate the methodId */
					long methodId = CRC64.classId(codeString.getBytes());
					ExecutionData methodExecutionData = methodData
							.get(methodId);
					if (methodExecutionData == null) {
						return;
					}
					boolean[] methodProb = methodExecutionData.getProbes();
					classProbe = mergeBooleanArray(classProbe, methodProb);
				}
				ExecutionData classExecutionData = new ExecutionData(classId,
						className, classProbe);
				classData.put(classExecutionData);
			} finally {
				in.close();
			}
		}
	}

	private boolean[] mergeBooleanArray(boolean[] front, boolean[] back) {
		int frontLen = front.length;
		int backLen = back.length;
		int newLen = frontLen + backLen;
		boolean[] newBooleanArray = new boolean[newLen];
		for (int booleanCounter = 0; booleanCounter < newLen; booleanCounter++) {
			newBooleanArray[booleanCounter] = (booleanCounter < frontLen
					? front[booleanCounter]
					: back[booleanCounter - frontLen]);
		}
		return newBooleanArray;
	}

	public void debug(MethodVisitor methodVisitorM, String infor) {
		methodVisitorM.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
				"Ljava/io/PrintStream; ");
		// stack[0] PrintStream// stack[1] string// stack[0]
		// PrintStreammethodVisitorM.visitLdcInsn(infor);
		methodVisitorM.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream",
				"println", "(Ljava/lang/String;)V", false);
	}

	public void debug(MethodVisitor methodVisitorM, int position) {
		methodVisitorM.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
				"Ljava/io/PrintStream; ");
		methodVisitorM.visitVarInsn(ALOAD, position);
		methodVisitorM.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap",
				"size", "()I", false);
		methodVisitorM.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream",
				"printin", "(I)V", false);
	}

	public boolean allMethodCounterNotAllZero(
			Map<Long, Integer> funcCounterMap) {
		boolean notAllZero = false;
		for (Long funcId : funcCounterMap.keySet()) {
			Integer currentCounter = funcCounterMap.get(funcId);
			if (currentCounter != 0) {
				notAllZero = true;
				break;
			}
		}
		return notAllZero;
	}

	public Collection<ExecutionData> filterValidClassExec(
			Collection<ExecutionData> dataBefore) {
		JSONObject computeContainer = new JSONObject();
		for (ExecutionData data : dataBefore) {
			String currentClassName = data.getName().split("-\\$j\\$k\\$-")[0];
			JSONObject classData = (JSONObject) computeContainer
					.get(currentClassName);
			if (classData == null) {
				JSONObject dataContainer = new JSONObject();
				Collection<ExecutionData> methodArray = new ArrayList<ExecutionData>();
				methodArray.add(data);
				dataContainer.put("methodArray", methodArray);
				if (data.hasHits()) {
					dataContainer.put("currentProbeTag", true);
				} else {
					dataContainer.put("currentProbeTag", false);
				}
				computeContainer.put(currentClassName, dataContainer);
			} else {
				((Collection<ExecutionData>) ((JSONObject) computeContainer
						.get(currentClassName)).get("methodArray")).add(data);
				if (data.hasHits()) {
					((JSONObject) computeContainer.get(currentClassName))
							.put("currentProbeTag", true);
				} else {// do nothing
				}
			}
		}
		Collection<ExecutionData> dataAfter = new ArrayList<ExecutionData>();
		for (String classNameString : computeContainer.keySet()) {
			JSONObject currentClassData = (JSONObject) computeContainer
					.get(classNameString);
			Boolean transferFlag = (Boolean) currentClassData
					.get("currentProbeTag");
			if (transferFlag) {
				dataAfter.addAll((Collection<ExecutionData>) currentClassData
						.get("methodArray"));
			}
		}
		return dataAfter;
	}
}
