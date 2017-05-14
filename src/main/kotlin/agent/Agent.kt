package agent

import jdk.internal.org.objectweb.asm.*
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class Agent : ClassFileTransformer {
    override fun transform(loader: ClassLoader?, className: String?, classBeingRedefined: Class<*>?,
                           protectionDomain: ProtectionDomain?, classfileBuffer: ByteArray?): ByteArray {
        val reader = ClassReader(classfileBuffer)
        val writer = ClassWriter(reader, 0)
        val visitor = TestClassVisitor(writer)
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }

    companion object {
        @JvmStatic
        fun premain(agentArgs: String?, inst: Instrumentation) {
            println("Agent started.")
            inst.addTransformer(Agent())
        }
    }
}

class TestClassVisitor(cv: ClassVisitor) : ClassVisitor (Opcodes.ASM5, cv) {
    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?,
                             exceptions: Array<out String>?): MethodVisitor {
        val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        return TestMethodVisitor(methodVisitor)
    }
}

class TestMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM5, mv) {
    private val testOwner = "example/CoroutineExampleKt"
    private val testDesc = "(Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;"
    private val testOpcode = Opcodes.INVOKESTATIC

    private var detected = false

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, desc: String?, itf: Boolean) {
        if (opcode == testOpcode && owner == testOwner && name == "test" && desc == testDesc) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
            mv.visitLdcInsn("Test detected")
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
            detected = true;
        }
        mv.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (detected) {
            mv.visitMaxs(maxStack + 2, maxLocals)
        } else {
            mv.visitMaxs(maxStack, maxLocals)
        }
    }
}
