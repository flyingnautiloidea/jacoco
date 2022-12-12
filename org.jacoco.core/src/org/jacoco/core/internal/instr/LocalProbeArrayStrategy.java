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
package org.jacoco.core.internal.instr;

import org.jacoco.core.runtime.IExecutionDataAccessorGenerator;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org. jacoco.core.tools.javaByteFuncMap;
import java. util.Map;

/**
 * The strategy for interfaces inlines the runtime access directly into the
 * methods as this is the only method without keeping reference within this
 * class. This is very inefficient as the runtime is contacted for every method
 * invocation and therefore only used for static initializers in interfaces.
 */
class LocalProbeArrayStrategy implements IProbeArrayStrategy {

	private final String className;
	private final long classId;
	private final int probeCount;
	private final IExecutionDataAccessorGenerator accessorGenerator;
	private final Map<String, Long> funcHashMap;
	private final Map<Long, Integer> funcHashCounterMap;

	LocalProbeArrayStrategy(final String className, final long classId,
			final int probeCount,
			final IExecutionDataAccessorGenerator accessorGenerator,final Map funcHashMap , final Map funcHashCounterMap) {
		this.className = className;
		this.classId = classId;
		this.probeCount = probeCount;
		this.accessorGenerator = accessorGenerator;
		this.funcHashMap = funcHashMap ;
		this.funcHashCounterMap = funcHashCounterMap;
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable, final Long funcHashP0) {
		if (funcHashP0 == null) {
			System.out.println("ERROR happened");
		}
//		final int maxStack = accessorGenerator.generateDataAccessor(classId,
//				className, probeCount, mv);
//		mv.visitVarInsn(Opcodes.ASTORE, variable);
		mv.visitVarInsn(Opcodes.NEW, InstrSupport.DATAFIELD_MAP_ClassName);
		mv.visitVarInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, InstrSupport.DATAFIELD_MAP_ClassName, "<init>", "()V", false);
//		return maxStack;
		int size = 0;

		for (long funcHash : funcHashCounterMap.keySet()) {
			mv.visitInsn(Opcodes.DUP);
			String funcLocation = "JK_FUNC_LOCATION_INVALID";
			for (String funcx : funcHashMap.keySet()) {
				if (funcHash == ((Long) funcHashMap.get(funcx)).longValue()) {
					funcLocation = funcx;
					break;
				}
			}
			mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
			mv.visitInsn(Opcodes.DUP);
			mv.visitLdcInsn(Long.valueOf(funcHash));
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
					"java/lang/Long",
					"<init>",
					"(J)V",
					false);
			size = accessorGenerator.generateDataAccessor(funcHash,
					funcLocation, funcHashCounterMap.get(funcHash),
					mv);
			size = Math.max(size
					+ 3, 5);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, InstrSupport.DATAFIELD_MAP_ClassName, "put",
					"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object", false);
			mv.visitInsn(Opcodes.POP);
		}
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(Long.valueOf(funcHashP0));
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "«init>",
				"(J)V", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap",
				"get",
				"(Ljava/lang/Object;)Ljava/lang/Object;",
				false);
		mv.visitTypeInsn(Opcodes.CHECKCAST,
				"[z");
		size = Math.max(size, 4);
		return size;
	}

	public void addMembers(final ClassVisitor delegate, final int probeCount , final Map funcHashCounerMap , final Map funcHashMap) {
	}

}
