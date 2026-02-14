use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use jni::sys::jbyteArray;
use prost::Message;

// Manually defined Protobuf structs to avoid prost-build dependency hell
pub mod enclave {
    #[derive(Clone, PartialEq, ::prost::Message)]
    pub struct IdentityRequest {
        #[prost(string, tag="1")]
        pub attribute_id: ::prost::alloc::string::String,
        #[prost(bytes="vec", tag="2")]
        pub encrypted_identity_seed: ::prost::alloc::vec::Vec<u8>,
    }

    #[derive(Clone, PartialEq, ::prost::Message)]
    pub struct EnclaveRequest {
        #[prost(string, tag="1")]
        pub request_id: ::prost::alloc::string::String,
        #[prost(string, tag="2")]
        pub action_type: ::prost::alloc::string::String,
        #[prost(bytes="vec", tag="3")]
        pub payload: ::prost::alloc::vec::Vec<u8>,
        #[prost(message, optional, tag="4")]
        pub identity_req: ::core::option::Option<IdentityRequest>,
    }

    #[derive(Clone, PartialEq, ::prost::Message)]
    pub struct EnclaveResponse {
        #[prost(string, tag="1")]
        pub request_id: ::prost::alloc::string::String,
        #[prost(bool, tag="2")]
        pub success: bool,
        #[prost(string, tag="3")]
        pub error_message: ::prost::alloc::string::String,
        #[prost(bytes="vec", tag="4")]
        pub proof_data: ::prost::alloc::vec::Vec<u8>,
    }
}

use enclave::{EnclaveRequest, EnclaveResponse, IdentityRequest};

#[no_mangle]
pub extern "system" fn Java_com_solanasuper_core_ZKProver_processEnclaveRequest(
    mut env: JNIEnv,
    _class: JClass,
    request_bytes: JByteArray,
) -> jbyteArray {
    // 1. Read input bytes from Java
    let input: Vec<u8> = match env.convert_byte_array(&request_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(), // Should throw exception ideally
    };

    // 2. Deserialize Protobuf
    let request = match EnclaveRequest::decode(&input[..]) {
        Ok(req) => req,
        Err(e) => {
            // Return error response
            let response = EnclaveResponse {
                request_id: "unknown".to_string(),
                success: false,
                error_message: format!("Failed to decode request: {}", e),
                proof_data: vec![],
            };
            return serialize_response(&mut env, response);
        }
    };

    // 3. Process Request (Mock Logic for now)
    let response = process_request_logic(request);

    // 4. Serialize and Return
    serialize_response(&mut env, response)
}

fn process_request_logic(request: EnclaveRequest) -> EnclaveResponse {
    // Dispatch based on action_type or presence of fields
    let proof_data = if let Some(identity_req) = request.identity_req {
        // Mock Identity Proof Generation
        // In real ZK, we'd verify the signature in encrypted_identity_seed matches the claim
        format!("identity_proof_for_{}", identity_req.attribute_id).into_bytes()
    } else {
        b"mock_zk_proof_data".to_vec()
    };
    
    EnclaveResponse {
        request_id: request.request_id,
        success: true,
        error_message: String::new(),
        proof_data,
    }
}

fn serialize_response(env: &mut JNIEnv, response: EnclaveResponse) -> jbyteArray {
    let mut output = Vec::new();
    if let Err(_) = response.encode(&mut output) {
        return std::ptr::null_mut();
    }

    match env.byte_array_from_slice(&output) {
        Ok(jbytes) => jbytes.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
