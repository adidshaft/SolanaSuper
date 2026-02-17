package com.solanasuper.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object SolanaUtil {
    // System Program ID: 11111111111111111111111111111111
    val SYSTEM_PROGRAM_ID = Base58.decode("11111111111111111111111111111111")

    fun longToLittleEndian(value: Long): ByteArray {
        val buffer = ByteBuffer.allocate(8)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(value)
        return buffer.array()
    }
    
    fun intToLittleEndian(value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(value)
        return buffer.array()
    }

    // Encodes length as ShortVec (Compact-u16)
    fun encodeLength(len: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        var rem_len = len
        while (true) {
            var elem = rem_len and 0x7f
            rem_len = rem_len shr 7
            if (rem_len == 0) {
                out.write(elem)
                break
            } else {
                elem = elem or 0x80
                out.write(elem)
            }
        }
        return out.toByteArray()
    }

    // Creates the "Message" part of the transaction (to be signed)
    fun createTransferMessage(
        fromPublicKey: ByteArray,
        toPublicKey: ByteArray,
        lamports: Long,
        recentBlockhash: ByteArray
    ): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        // 1. Header
        // numRequiredSignatures = 1 (Sender)
        // numReadonlySignedAccounts = 0
        // numReadonlyUnsignedAccounts = 1 (System Program)
        out.write(1)
        out.write(0)
        out.write(1)

        // 2. Account Addresses
        // We need 3 accounts: [Sender, Recipient, SystemProgram]
        // Sender must be first (index 0) because it signs.
        // Recipient is writable (index 1).
        // System Program is readonly (index 2).
        
        // Count = 3
        out.write(encodeLength(3))
        out.write(fromPublicKey)
        out.write(toPublicKey)
        out.write(SYSTEM_PROGRAM_ID)

        // 3. Recent Blockhash
        out.write(recentBlockhash)

        // 4. Instructions
        // Count = 1
        out.write(encodeLength(1))

        // Instruction 0:
        // Program ID Index: 2 (System Program)
        out.write(2) 
        
        // Account Indices Count = 2 (Sender, Recipient)
        out.write(encodeLength(2))
        out.write(0) // Sender (Signer, Writable)
        out.write(1) // Recipient (Writable)

        // Data Length
        // System Program Transfer Layout:
        //  - u32: Instruction Index (2 for Transfer)
        //  - u64: Lamports
        // Total = 4 + 8 = 12 bytes
        out.write(encodeLength(12))
        
        // Data
        out.write(intToLittleEndian(2)) // Instruction Index 2 = Transfer
        out.write(longToLittleEndian(lamports))

        return out.toByteArray()
    }

    // Combines Signature and Message into Final Transaction
    fun encodeTransaction(signature: ByteArray, message: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        
        // 1. Signature Count = 1
        out.write(encodeLength(1))
        
        // 2. Signature (64 bytes)
        out.write(signature)
        
        // 3. Message
        out.write(message)
        
        return out.toByteArray()
    }
}
