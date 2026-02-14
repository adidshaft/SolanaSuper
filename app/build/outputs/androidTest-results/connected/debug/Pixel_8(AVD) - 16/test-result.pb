

emulator-5554primary°
ï
IdentityKeyManagerTestcom.solanasuper.security+generateKey_shouldRequireUserAuthentication2•Û¬Ã¿Ù≠Â:•Û¬ÃÄÂ£ÒB
emulator-5554primary¡
ç	java.security.InvalidAlgorithmParameterException: java.lang.IllegalStateException: At least one biometric must be enrolled to create keys requiring user authentication for every use
at android.security.keystore2.AndroidKeyStoreKeyPairGeneratorSpi.initialize(AndroidKeyStoreKeyPairGeneratorSpi.java:368)
at java.security.KeyPairGenerator$Delegate.initialize(KeyPairGenerator.java:720)
at java.security.KeyPairGenerator.initialize(KeyPairGenerator.java:461)
at com.solanasuper.security.IdentityKeyManager.generateIdentityKey(IdentityKeyManager.kt:37)
at com.solanasuper.security.IdentityKeyManagerTest.generateKey_shouldRequireUserAuthentication(IdentityKeyManagerTest.kt:15)
... 32 trimmed
Caused by: java.lang.IllegalStateException: At least one biometric must be enrolled to create keys requiring user authentication for every use
at android.security.keystore2.KeyStore2ParameterUtils.addSids(KeyStore2ParameterUtils.java:269)
at android.security.keystore2.KeyStore2ParameterUtils.addUserAuthArgs(KeyStore2ParameterUtils.java:330)
at android.security.keystore2.AndroidKeyStoreKeyPairGeneratorSpi.initialize(AndroidKeyStoreKeyPairGeneratorSpi.java:366)
... 37 more
java.lang.IllegalStateExceptionç	java.security.InvalidAlgorithmParameterException: java.lang.IllegalStateException: At least one biometric must be enrolled to create keys requiring user authentication for every use
at android.security.keystore2.AndroidKeyStoreKeyPairGeneratorSpi.initialize(AndroidKeyStoreKeyPairGeneratorSpi.java:368)
at java.security.KeyPairGenerator$Delegate.initialize(KeyPairGenerator.java:720)
at java.security.KeyPairGenerator.initialize(KeyPairGenerator.java:461)
at com.solanasuper.security.IdentityKeyManager.generateIdentityKey(IdentityKeyManager.kt:37)
at com.solanasuper.security.IdentityKeyManagerTest.generateKey_shouldRequireUserAuthentication(IdentityKeyManagerTest.kt:15)
... 32 trimmed
Caused by: java.lang.IllegalStateException: At least one biometric must be enrolled to create keys requiring user authentication for every use
at android.security.keystore2.KeyStore2ParameterUtils.addSids(KeyStore2ParameterUtils.java:269)
at android.security.keystore2.KeyStore2ParameterUtils.addUserAuthArgs(KeyStore2ParameterUtils.java:330)
at android.security.keystore2.AndroidKeyStoreKeyPairGeneratorSpi.initialize(AndroidKeyStoreKeyPairGeneratorSpi.java:366)
... 37 more
"Ì

logcatandroid◊
‘/Users/amanpandey/Desktop/SolanaSuper/app/build/outputs/androidTest-results/connected/debug/Pixel_8(AVD) - 16/logcat-com.solanasuper.security.IdentityKeyManagerTest-generateKey_shouldRequireUserAuthentication.txt"ò

device-infoandroid~
|/Users/amanpandey/Desktop/SolanaSuper/app/build/outputs/androidTest-results/connected/debug/Pixel_8(AVD) - 16/device-info.pb"ô

device-info.meminfoandroidw
u/Users/amanpandey/Desktop/SolanaSuper/app/build/outputs/androidTest-results/connected/debug/Pixel_8(AVD) - 16/meminfo"ô

device-info.cpuinfoandroidw
u/Users/amanpandey/Desktop/SolanaSuper/app/build/outputs/androidTest-results/connected/debug/Pixel_8(AVD) - 16/cpuinfo*ˇ
c
test-results.logOcom.google.testing.platform.runtime.android.driver.AndroidInstrumentationDriverâ
Ü/Users/amanpandey/Desktop/SolanaSuper/app/build/outputs/androidTest-results/connected/debug/Pixel_8(AVD) - 16/testlog/test-results.log 2
text/plain