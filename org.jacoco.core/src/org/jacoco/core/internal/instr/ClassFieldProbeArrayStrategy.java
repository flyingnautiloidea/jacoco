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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

/**
 * The strategy for regular classes adds a static field to hold the probe array
 * and a static initialization method requesting the probe array from the
 * runtime.
 */
class ClassFieldProbeArrayStrategy implements IProbeArrayStrategy {

	/**
	 * Frame stack with a single boolean array.
	 */
	private static final Object[] FRAME_STACK_ARRZ = new Object[] {
			InstrSupport.DATAFIELD_MAP_ClassName };

	/**
	 * Empty frame locals.
	 */
	private static final Object[] FRAME_LOCALS_EMPTY = new Object[0];

	private final String className;
	private final long classId;
	private final boolean withFrames;
	private final IExecutionDataAccessorGenerator accessorGenerator;

	ClassFieldProbeArrayStrategy(final String className, final long classId,
			final boolean withFrames,
			final IExecutionDataAccessorGenerator accessorGenerator) {
		this.className = className;
		this.classId = classId;
		this.withFrames = withFrames;
		this.accessorGenerator = accessorGenerator;
	}

	public int storeInstance(final MethodVisitor mv, final boolean clinit,
			final int variable, final Long funcHash) {
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(Long.valueOf(funcHash));
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "<init>",
				"(J)V", false);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.INITMETHOD_NAME_QUERYMAP,
				InstrSupport.INITMETHOD_DESC_QUERYMAP, false);
		mv.visitVarInsn(Opcodes.ASTORE, variable);
		int size = 4;
		return size;
	}

	public void addMembers(final ClassVisitor cv, final int probeCount,
			final Map funcHashCounterMap, final Map funcHashMap) {
		createDataField(cv);
		createInitMethod(cv, funcHashCounterMap, funcHashMap);
		createQueryMapMethod(cv);
	}

	private void createDataField(final ClassVisitor cv) {
		cv.visitField(InstrSupport.DATAFIELD_ACC, InstrSupport.DATAFIELD_NAME,
				InstrSupport.DATAFIELD_MAP_DESC, null, null);
	}

	private void createInitMethod(final ClassVisitor cv,
			final Map funcHashCounterMap, final Map funcHashMap) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME, InstrSupport.INITMETHOD_DESC,
				null, null);
		mv.visitCode();

		// Load the value of the static data field:
		mv.visitFieldInsn(Opcodes.GETSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_MAP_DESC);
		mv.visitInsn(Opcodes.DUP);

		// Stack[1]: Map
		// Stack[0]: Map

		// Skip initialization when we already have a data array:
		final Label alreadyInitialized = new Label();
		mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

		// Stack[0]: Map

		mv.visitInsn(Opcodes.POP);
		final int size = genInitializeDataFieldJK(mv, funcHashCounterMap,
				funcHashMap);

		// Stack[0]: [Z

		// Return the class' probe array:
		if (withFrames) {
			mv.visitFrame(Opcodes.F_NEW, 0, FRAME_LOCALS_EMPTY, 1,
					FRAME_STACK_ARRZ);
		}
		mv.visitLabel(alreadyInitialized);
		mv.visitInsn(Opcodes.ARETURN);

		mv.visitMaxs(Math.max(size, 2), 0); // Maximum local stack size is 2
		mv.visitEnd();
	}

	/**
	 * Generates the byte code to initialize the static coverage data field
	 * within this class.
	 *
	 * The code will push the [Z data array on the operand stack.
	 *
	 * @param mv
	 *            generator to emit code to
	 */
	private int genInitializeDataField(final MethodVisitor mv,
			final int probeCount) {
		final int size = accessorGenerator.generateDataAccessor(classId,
				className, probeCount, mv);
		mv.visitInsn(Opcodes.DUP);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_DESC);
		return Math.max(size, 2); // Maximum local stack size is 2
	}

	private int genInitializeDataFieldJK(final MethodVisitor mv,
			final Map<Long, Integer> funcHashCounterMap,
			final Map<String, Long> funcHashMap) {
		mv.visitTypeInsn(Opcodes.NEW, InstrSupport.DATAFIELD_MAP_ClassName);
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
				InstrSupport.DATAFIELD_MAP_ClassName, "<init>", "()v", false);
		int size = 0;
		for (long funcHash : funcHashCounterMap.keySet()) {
			mv.visitInsn(Opcodes.DUP);
			String funcLocation = "initial funcLocation";
			for (String funcx : funcHashMap.keySet()) {
				if (funcHash == ((Long) funcHashMap.get(funcx)).longValue()) {
					funcLocation = funcx;
					break;
				}
			}

			mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
			mv.visitInsn(Opcodes.DUP);
			mv.visitLdcInsn(Long.valueOf(funcHash));
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long",
					"‹init>", "(J)V", false);
			size = accessorGenerator.generateDataAccessor(funcHash,
					funcLocation, funcHashCounterMap.get(funcHash), mv);
			size = Math.max(size + 3, 5);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
					InstrSupport.DATAFIELD_MAP_ClassName, "put",
					"(Ljava/lang/Object;Ljava/Lang/Object;)Ljava/lang/Object;",
					false);
			mv.visitInsn(Opcodes.POP);
		}
		mv.visitInsn(Opcodes.DUP);
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className,
				InstrSupport.DATAFIELD_NAME, InstrSupport.DATAFIELD_MAP_DESC);
		return Math.max(size, 2);
	}

	/**
	 * this functions is used by biz functions to get the (Z in the map
	 *
	 * @param cv
	 */
	private void createQueryMapMethod(final ClassVisitor cv) {
		final MethodVisitor mv = cv.visitMethod(InstrSupport.INITMETHOD_ACC,
				InstrSupport.INITMETHOD_NAME_QUERYMAP,
				InstrSupport.INITMETHOD_DESC_QUERYMAP, null, null);
		mv.visitCode();
		// Load the value of the static data field:
		// mv. visitFieldInsn(Opcodes. GETSTATIC, className,
		// InstrSupport.DATAFIELD_NAME,InstrSupport.DATAFIELD_MAP_DESC);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, className,
				InstrSupport.INITMETHOD_NAME, InstrSupport.INITMETHOD_DESC,
				false);

		/**
		 * get funcHash from localTable , and use it to get (Z from the Map then
		 * return it
		 **/
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/MashMap", "get",
				"(Ljava/lang/Object:)Ljava/lang/Object;", false);
		mv.visitTypeInsn(Opcodes.CHECKCAST, "[Z");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(2, 1); // Maximum local stack size is 2
		mv.visitEnd();
	}

}
