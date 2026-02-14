
use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use prost::Message;

// Include the generated protobuf modules
pub mod enclave {
    include!(concat!(env!("OUT_DIR"), "/enclave.rs"));
}

use enclave::{EnclaveRequest, EnclaveResponse, enclave_request::Payload};

#[no_mangle]
pub extern "system" fn Java_com_solanasuper_core_ZKProver_processEnclaveRequest(
    mut env: JNIEnv,
    _class: JClass,
    request_bytes: JByteArray,
) -> jbyteArray {
    // 1. Read input bytes
    let input: Vec<u8> = match env.convert_byte_array(&request_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(), // Should throw exception
    };

    // 2. Decode Protobuf
    let request = match EnclaveRequest::decode(&input[..]) {
        Ok(req) => req,
        Err(e) => return create_error_response(&mut env, &format!("Decode error: {}", e)),
    };

    // 3. Process Logic (Mock ZK)
    let response = process_logic(request);

    // 4. Encode Response
    let mut output = Vec::new();
    if let Err(e) = response.encode(&mut output) {
        return create_error_response(&mut env, &format!("Encode error: {}", e));
    }

    // 5. Return as Java ByteArray
    match env.byte_array_from_slice(&output) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

fn process_logic(request: EnclaveRequest) -> EnclaveResponse {
    let mut response = EnclaveResponse {
        success: true,
        zk_proof: vec![0xCA, 0xFE, 0xBA, 0xBE], // Mock proof
        error_message: String::new(),
    };

    match request.payload {
        Some(Payload::IdentityReq(req)) => {
            if req.attribute_id == "invalid" {
                 response.success = false;
                 response.error_message = "Invalid attribute".to_string();
            }
            // In real ZK, we would verify encrypted_identity_seed here
        },
        Some(Payload::GovernanceReq(_)) => {
            // Mock vote proof
        },
        Some(Payload::IncomeReq(_)) => {
            // Mock payment proof
        },
        Some(Payload::HealthReq(_)) => {
             // Mock health proof
        },
        None => {
            response.success = false;
            response.error_message = "Empty payload".to_string();
        }
    }

    response
}

fn create_error_response(env: &mut JNIEnv, msg: &str) -> jbyteArray {
    let response = EnclaveResponse {
        success: false,
        zk_proof: vec![],
        error_message: msg.to_string(),
    };
    let mut output = Vec::new();
    let _ = response.encode(&mut output);
    match env.byte_array_from_slice(&output) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
