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

import org.jacoco.core.internal.flow.ClassProbesVisitor;
import org.jacoco.core.internal.flow.MethodProbesVisitor;
import org.jacoco.core.tools.javaByteFunctionMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
/**
 * Adapter that instruments a class for coverage tracing.
 */
public class ClassInstrumenter extends ClassProbesVisitor {

	private final IProbeArrayStrategy probeArrayStrategy;

	private String className;
	private final Map<String,Long> funcHashMap ;
	javaByteFunctionMap jbf = new javaByteFunctionMap();

	/**
	 * Emits a instrumented version of this class to the given class visitor.
	 *
	 * @param probeArrayStrategy
	 *            this strategy will be used to access the probe array
	 * @param cv
	 *            next delegate in the visitor chain will receive the
	 *            instrumented class
	 */
	public ClassInstrumenter(final IProbeArrayStrategy probeArrayStrategy,
			final ClassVisitor cv , final Map funcHashMap) {
		super(cv);
		this.probeArrayStrategy = probeArrayStrategy;
		this.funcHashMap = funcHashMap ;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		this.className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(final int access, final String name,
			final String desc, final String signature, final Object value) {
		InstrSupport.assertNotInstrumented(name, className);
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodProbesVisitor visitMethod(final int access, final String name,
			final String desc, final String signature,
			final String[] exceptions) {

		InstrSupport.assertNotInstrumented(name, className);

		final MethodVisitor mv = cv.visitMethod(access, name, desc, signature,
				exceptions);
		String funcHashkey = jbf.keyBuilderForQuery(className, name, desc);
		Long funcHash = funcHashMap. get (funcHashkey);
		if (funcHash == null) {
			System.out.println(" (((((((((((classname:" + className +
					"))))))))))))methodName::::"
							+ name + "(((((((key:"
							+ funcHashkey + "((((((hash:" + funcHash);
		}

		if (mv == null) {
			return null;
		}
		final MethodVisitor frameEliminator = new DuplicateFrameEliminator(mv);
		final ProbeInserter probeVariableInserter = new ProbeInserter(access,
				name, desc, frameEliminator, probeArrayStrategy,funcHash);
		return new MethodInstrumenter(probeVariableInserter,
				probeVariableInserter,funcHash);
	}

	@Override
	public void visitTotalProbeCount(final int count , final Map funcHashCounterMap , final Map funcHashMap) {
		probeArrayStrategy.addMembers(cv, count,funcHashCounterMap ,funcHashMap);
	}

}
