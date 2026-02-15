
use std::io::Result;

fn main() -> Result<()> {
    // Compile the enclave.proto file
    // We assume the proto file is at ../../proto/enclave.proto relative to crate root (app/src/main/cpp/rust)
    prost_build::compile_protos(&["../../proto/enclave.proto"], &["../../proto/"])?;
    Ok(())
}
