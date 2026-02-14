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
    pub struct GovernanceRequest {
        #[prost(string, tag="1")]
        pub proposal_id: ::prost::alloc::string::String,
        #[prost(string, tag="2")]
        pub vote_choice: ::prost::alloc::string::String,
        #[prost(bytes="vec", tag="3")]
        pub identity_signature: ::prost::alloc::vec::Vec<u8>,
    }

    #[derive(Clone, PartialEq, ::prost::Message)]
    pub struct IncomeRequest {
        #[prost(int64, tag="1")]
        pub amount: i64,
        #[prost(string, tag="2")]
        pub receiver_pubkey: ::prost::alloc::string::String,
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
        #[prost(message, optional, tag="5")]
        pub governance_req: ::core::option::Option<GovernanceRequest>,
        #[prost(message, optional, tag="6")]
        pub income_req: ::core::option::Option<IncomeRequest>,
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

use enclave::{EnclaveRequest, EnclaveResponse, IdentityRequest, GovernanceRequest, IncomeRequest};

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
        format!("identity_proof_for_{}", identity_req.attribute_id).into_bytes()
    } else if let Some(governance_req) = request.governance_req {
        // Mock MPC Vote Share Generation
        // In real MPC, we'd split the vote into shares encrypted for Arcium nodes
        format!("mpc_vote_share_for_{}", governance_req.proposal_id).into_bytes()
    } else if let Some(income_req) = request.income_req {
        // Mock Income Proof Generation (Double-Spend Protection)
        // In real ZK, this proves we have the private key for the funds and haven't spent them (via nullifier)
        format!("zk_income_proof_for_{}_amount_{}", income_req.receiver_pubkey, income_req.amount).into_bytes()
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
