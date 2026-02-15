package net.rossonet.waldot.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import samples.CmdLine;
import samples.DrsServer;
import tss.Crypto;
import tss.Helpers;
import tss.Tpm;
import tss.TpmBuffer;
import tss.TpmDeviceTcp;
import tss.TpmException;
import tss.TpmFactory;
import tss.TpmHelpers;
import tss.tpm.CreatePrimaryResponse;
import tss.tpm.CreateResponse;
import tss.tpm.DuplicateResponse;
import tss.tpm.EncryptDecryptResponse;
import tss.tpm.GetCapabilityResponse;
import tss.tpm.MakeCredentialResponse;
import tss.tpm.ReadPublicResponse;
import tss.tpm.StartAuthSessionResponse;
import tss.tpm.TPM2B_DATA;
import tss.tpm.TPM2B_DIGEST;
import tss.tpm.TPM2B_DIGEST_KEYEDHASH;
import tss.tpm.TPM2B_ENCRYPTED_SECRET;
import tss.tpm.TPM2B_ID_OBJECT;
import tss.tpm.TPM2B_PRIVATE;
import tss.tpm.TPM2B_PUBLIC;
import tss.tpm.TPM2B_PUBLIC_KEY_RSA;
import tss.tpm.TPMA_OBJECT;
import tss.tpm.TPML_HANDLE;
import tss.tpm.TPMS_KEYEDHASH_PARMS;
import tss.tpm.TPMS_NULL_ASYM_SCHEME;
import tss.tpm.TPMS_PCR_SELECTION;
import tss.tpm.TPMS_RSA_PARMS;
import tss.tpm.TPMS_SCHEME_HMAC;
import tss.tpm.TPMS_SENSITIVE_CREATE;
import tss.tpm.TPMS_SIGNATURE_RSASSA;
import tss.tpm.TPMS_SIG_SCHEME_RSASSA;
import tss.tpm.TPMS_SYMCIPHER_PARMS;
import tss.tpm.TPMT_PUBLIC;
import tss.tpm.TPMT_SYM_DEF;
import tss.tpm.TPMT_SYM_DEF_OBJECT;
import tss.tpm.TPMU_SIGNATURE;
import tss.tpm.TPM_ALG_ID;
import tss.tpm.TPM_CAP;
import tss.tpm.TPM_CC;
import tss.tpm.TPM_HANDLE;
import tss.tpm.TPM_HT;
import tss.tpm.TPM_PT;
import tss.tpm.TPM_RC;
import tss.tpm.TPM_RH;
import tss.tpm.TPM_SE;
import tss.tpm.TPM_SU;

public class TpmHelper {

	public static final TPMT_SYM_DEF_OBJECT Aes128SymDef = new TPMT_SYM_DEF_OBJECT(TPM_ALG_ID.AES, 128, TPM_ALG_ID.CFB);
	public static final TPM_HANDLE EK_PersHandle = TPM_HANDLE.persistent(0x00010001);
	public static final TPMT_PUBLIC EK_Template = new TPMT_PUBLIC(
			// TPMI_ALG_HASH nameAlg
			TPM_ALG_ID.SHA256,
			// TPMA_OBJECT objectAttributes
			new TPMA_OBJECT(TPMA_OBJECT.restricted, TPMA_OBJECT.decrypt, TPMA_OBJECT.fixedTPM, TPMA_OBJECT.fixedParent,
					TPMA_OBJECT.adminWithPolicy, TPMA_OBJECT.sensitiveDataOrigin),
			// TPM2B_DIGEST authPolicy
			Helpers.fromHex("837197674484b3f81a90cc8d46a5d724fd52d76e06520b64f2a1da1b331469aa"),
			// TPMU_PUBLIC_PARMS parameters
			new TPMS_RSA_PARMS(Aes128SymDef, new TPMS_NULL_ASYM_SCHEME(), 2048, 0),
			// TPMU_PUBLIC_ID unique
			new TPM2B_PUBLIC_KEY_RSA());
	public static final TPM_HANDLE ID_KEY_PersHandle = TPM_HANDLE.persistent(0x00000100);
	public static TPMS_SENSITIVE_CREATE IdKeySens = null;
	public static TPMT_PUBLIC IdKeyTemplate = null;
	public static final TPMT_SYM_DEF_OBJECT NullSymDef = new TPMT_SYM_DEF_OBJECT(TPM_ALG_ID.AES, 128, TPM_ALG_ID.CFB);
	public static final TPM_HANDLE SRK_PersHandle = TPM_HANDLE.persistent(0x00000001);
	public static final TPMT_PUBLIC SRK_Template = new TPMT_PUBLIC(
			// TPMI_ALG_HASH nameAlg
			TPM_ALG_ID.SHA256,
			// TPMA_OBJECT objectAttributes
			new TPMA_OBJECT(TPMA_OBJECT.restricted, TPMA_OBJECT.decrypt, TPMA_OBJECT.fixedTPM, TPMA_OBJECT.fixedParent,
					TPMA_OBJECT.noDA, TPMA_OBJECT.userWithAuth, TPMA_OBJECT.sensitiveDataOrigin),
			// TPM2B_DIGEST authPolicy
			new byte[0],
			// TPMU_PUBLIC_PARMS parameters
			new TPMS_RSA_PARMS(Aes128SymDef, new TPMS_NULL_ASYM_SCHEME(), 2048, 0),
			// TPMU_PUBLIC_ID unique
			new TPM2B_PUBLIC_KEY_RSA());

	public static boolean allSlotsEmpty(Tpm tpm) {
		boolean slotFull = false;
		GetCapabilityResponse resp = tpm.GetCapability(TPM_CAP.HANDLES, TPM_HT.TRANSIENT.toInt() << 24, 32);
		TPML_HANDLE handles = (TPML_HANDLE) resp.capabilityData;
		if (handles.handle.length != 0) {
			print("Objects remain:" + String.valueOf(handles.handle.length));
			slotFull = true;
		}
		resp = tpm.GetCapability(TPM_CAP.HANDLES, TPM_HT.LOADED_SESSION.toInt() << 24, 32);
		handles = (TPML_HANDLE) resp.capabilityData;
		if (handles.handle.length != 0) {
			print("Sessions remain:" + String.valueOf(handles.handle.length));
			slotFull = true;
		}
		return slotFull;
	}

	public static void clearPersistent(Tpm tpm, TPM_HANDLE hPers, String keyRole) {
		tpm._allowErrors().ReadPublic(hPers);
		final TPM_RC rc = tpm._getLastResponseCode();
		if (rc == TPM_RC.SUCCESS) {
			print("Deleting persistent %s 0x%08X", keyRole, hPers.handle);
			tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), hPers, hPers);
			print("Successfully deleted persistent %s 0x%08X", keyRole, hPers.handle);
		} else if (rc == TPM_RC.HANDLE) {
			print("%s 0x%08X does not exist", keyRole, hPers.handle);
		} else {
			print("Unexpected failure <%s> of TPM2_ReadPublic for %s 0x%08X", rc, keyRole, hPers.handle);
		}
	}

	public static void close(Tpm tpm) throws IOException {
		if (tpm._getDevice() instanceof TpmDeviceTcp) {
			tpm.Shutdown(TPM_SU.CLEAR);
		}
		tpm.close();
	}

	public static TPM_HANDLE createPersistentEkRsa(Tpm tpm, int handle) {
		final TPMT_PUBLIC ekTmpl = EK_Template;
		final CreatePrimaryResponse rsp = tpm.CreatePrimary(TPM_HANDLE.from(TPM_RH.ENDORSEMENT),
				new TPMS_SENSITIVE_CREATE(), ekTmpl, null, null);
		final TPM_HANDLE persistent = TPM_HANDLE.persistent(handle);
		tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), rsp.handle, persistent);
		tpm.FlushContext(rsp.handle);
		return persistent;
	}

	public static TPM_HANDLE createPersistentEkRsa(Tpm tpm, int handle, int rsaSigningTemplateBits) {
		final TPMT_PUBLIC ekTmpl = createRsaSigningTemplate(rsaSigningTemplateBits);
		final CreatePrimaryResponse rsp = tpm.CreatePrimary(TPM_HANDLE.from(TPM_RH.ENDORSEMENT),
				new TPMS_SENSITIVE_CREATE(), ekTmpl, null, null);
		final TPM_HANDLE persistent = TPM_HANDLE.persistent(handle);
		tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), rsp.handle, persistent);
		tpm.FlushContext(rsp.handle);
		return persistent;
	}

	public static TPMT_PUBLIC createPersistentPrimary(Tpm tpm, TPM_HANDLE hPers, TPM_RH hierarchy, TPMT_PUBLIC inPub,
			String primaryRole) {
		final ReadPublicResponse rpResp = tpm._allowErrors().ReadPublic(hPers);
		final TPM_RC rc = tpm._getLastResponseCode();
		if (rc == TPM_RC.SUCCESS) {
			// todo: Check if the public area of the existing key matches the requested one
			print(">> %s already exists\r\n", primaryRole);
			return rpResp.outPublic;
		}
		if (rc != TPM_RC.HANDLE) {
			print("Unexpected failure {%s} of TPM2_ReadPublic for %s 0x%08X", rc.name(), primaryRole, hPers);
			return null; // TPM_RH_NULL
		}
		final TPMS_SENSITIVE_CREATE sens = new TPMS_SENSITIVE_CREATE(new byte[0], new byte[0]);
		final CreatePrimaryResponse cpResp = tpm.CreatePrimary(TPM_HANDLE.from(hierarchy), sens, inPub, new byte[0],
				new TPMS_PCR_SELECTION[0]);
		print(">> Successfully created transient %s 0x%08X\r\n", primaryRole, cpResp.handle.handle);
		tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), cpResp.handle, hPers);
		print(">> Successfully persisted %s as 0x%08X\r\n", primaryRole, hPers.handle);
		tpm.FlushContext(cpResp.handle);
		return cpResp.outPublic;
	}

	public static TPM_HANDLE createPersistentSrk(Tpm tpm, int handle, byte[] ownerAuth) {
		final TPMT_PUBLIC srkTmpl = SRK_Template;
		final CreatePrimaryResponse rsp = tpm.CreatePrimary(TPM_HANDLE.from(TPM_RH.OWNER), new TPMS_SENSITIVE_CREATE(),
				srkTmpl, null, null);
		final TPM_HANDLE persistent = TPM_HANDLE.persistent(handle);
		tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), rsp.handle, persistent);
		tpm.FlushContext(rsp.handle);
		return persistent;
	}

	public static TPMT_PUBLIC createRsaSigningTemplate(int bits) {
		final TPMT_PUBLIC t = new TPMT_PUBLIC(TPM_ALG_ID.SHA256,
				new TPMA_OBJECT(TPMA_OBJECT.sign, TPMA_OBJECT.fixedParent, TPMA_OBJECT.fixedTPM,
						TPMA_OBJECT.sensitiveDataOrigin, TPMA_OBJECT.userWithAuth),
				new byte[0],
				new TPMS_RSA_PARMS(new TPMT_SYM_DEF_OBJECT(TPM_ALG_ID.NULL, 0, TPM_ALG_ID.NULL),
						new TPMS_SIG_SCHEME_RSASSA(TPM_ALG_ID.SHA256), bits, 65537),
				new TPM2B_PUBLIC_KEY_RSA(new byte[(bits / 8)]));
		return t;
	}

	public static int getActivationBlob(Tpm tpm, byte[] ekPubBlob, int ekPubSize, byte[] srkPubBlob, int srkPubSize,
			byte[] actBlobBuffer, int blobBufCapacity) {
		final TPMT_PUBLIC ekPub = TPM2B_PUBLIC.fromBytes(ekPubBlob).publicArea;
		final TPMT_PUBLIC srkPub = TPM2B_PUBLIC.fromBytes(srkPubBlob).publicArea;
		// Start a policy session required for key duplication
		final TPM_HANDLE sess = tpm.StartAuthSession(TPM_HANDLE.NULL, TPM_HANDLE.NULL, Helpers.RandomBytes(20),
				new byte[0], TPM_SE.POLICY, new TPMT_SYM_DEF(TPM_ALG_ID.NULL, 0, TPM_ALG_ID.NULL),
				TPM_ALG_ID.SHA256).handle;
		// Run the necessary policy command
		tpm.PolicyCommandCode(sess, TPM_CC.Duplicate);
		// Retrieve the policy digest computed by the TPM
		final byte[] dupPolicyDigest = tpm.PolicyGetDigest(sess);
		IdKeyTemplate = new TPMT_PUBLIC(TPM_ALG_ID.SHA256,
				new TPMA_OBJECT(TPMA_OBJECT.sign, TPMA_OBJECT.userWithAuth, TPMA_OBJECT.noDA), dupPolicyDigest,
				new TPMS_KEYEDHASH_PARMS(new TPMS_SCHEME_HMAC(TPM_ALG_ID.SHA256)), new TPM2B_DIGEST_KEYEDHASH());
		final byte[] keyBytes = Helpers.RandomBytes(32);
		IdKeySens = new TPMS_SENSITIVE_CREATE(new byte[0], keyBytes);
		final CreatePrimaryResponse idKey = tpm.CreatePrimary(TPM_HANDLE.from(TPM_RH.OWNER), IdKeySens, IdKeyTemplate,
				new byte[0], new TPMS_PCR_SELECTION[0]);
		final TPM_HANDLE srkPubHandle = tpm.LoadExternal(null, srkPub, TPM_HANDLE.from(TPM_RH.OWNER));
		final TPMT_SYM_DEF_OBJECT symWrapperDef = new TPMT_SYM_DEF_OBJECT(TPM_ALG_ID.AES, 128, TPM_ALG_ID.CFB);
		final DuplicateResponse dupResp = tpm._withSession(sess).Duplicate(idKey.handle, srkPubHandle, new byte[0],
				symWrapperDef);
		tpm.FlushContext(srkPubHandle);
		final TPM_HANDLE ekPubHandle = tpm.LoadExternal(null, ekPub, TPM_HANDLE.from(TPM_RH.ENDORSEMENT));
		final MakeCredentialResponse cred = tpm.MakeCredential(ekPubHandle, dupResp.encryptionKeyOut, srkPub.getName());
		// Delete the key and session handles
		tpm.FlushContext(ekPubHandle);
		tpm.FlushContext(idKey.handle);
		tpm.FlushContext(sess);
		final TPMT_PUBLIC symWrapperTemplate = new TPMT_PUBLIC(TPM_ALG_ID.SHA256,
				new TPMA_OBJECT(TPMA_OBJECT.decrypt, TPMA_OBJECT.encrypt, TPMA_OBJECT.userWithAuth), new byte[0],
				new TPMS_SYMCIPHER_PARMS(symWrapperDef), new TPM2B_DIGEST());
		//
		// Encrypt URI data to be passed to the client device
		//
		final TPMS_SENSITIVE_CREATE sens = new TPMS_SENSITIVE_CREATE(new byte[0], dupResp.encryptionKeyOut);
		final TPM_HANDLE symWrapperHandle = tpm.CreatePrimary(TPM_HANDLE.from(TPM_RH.OWNER), sens, symWrapperTemplate,
				new byte[0], new TPMS_PCR_SELECTION[0]).handle;
		final byte[] uriData = "http://my.test.url/TestDeviceID=F4ED90771DAA7C0B3230FF675DF8A61104AE7C8BB0093FD6A"
				.getBytes(); // Charset.forName("UTF-8")
		final byte[] iv = new byte[dupResp.encryptionKeyOut.length];
		final byte[] encryptedUri = tpm.EncryptDecrypt(symWrapperHandle, (byte) 0, TPM_ALG_ID.CFB, iv, uriData).outData;
		// Delete the key and session handles
		tpm.FlushContext(symWrapperHandle);
		//
		// Build activation blob for the client device
		//
		final TpmBuffer actBlob = new TpmBuffer();
		final byte[] credBlob = cred.credentialBlob.toBytes();
		actBlob.writeShort(credBlob.length);
		actBlob.writeByteBuf(credBlob);
		actBlob.writeShort(cred.secret.length);
		actBlob.writeByteBuf(cred.secret);
		dupResp.duplicate.toTpm(actBlob);
		actBlob.writeShort(dupResp.outSymSeed.length);
		actBlob.writeByteBuf(dupResp.outSymSeed);
		final byte[] idKeyPub = idKey.outPublic.toBytes();
		actBlob.writeShort(idKeyPub.length);
		actBlob.writeByteBuf(idKeyPub);
		actBlob.writeShort(encryptedUri.length);
		actBlob.writeByteBuf(encryptedUri);
		System.arraycopy(actBlob.buffer(), 0, actBlobBuffer, 0, actBlob.curPos());
		return actBlob.curPos();
	}

	public static byte[] getRandomBytes(Tpm tpm, int bytesRequested) {
		return tpm.GetRandom(bytesRequested);
	}

	public static byte[] hash(Tpm tpm, byte[] data) {
		return tpm.Hash(data, TPM_ALG_ID.SHA256, TPM_HANDLE.NULL).outHash;
	}

	public static byte[] hmac(Tpm tpm, TPM_HANDLE keyHandle, byte[] data) {
		return tpm.HMAC(keyHandle, data, TPM_ALG_ID.SHA256);
	}

	public static Tpm localTpmSimulator() {
		return TpmFactory.localTpmSimulator();
	}

	public static Tpm platformTpm() {
		return TpmFactory.platformTpm();
	}

	public static void print(String fmt, Object... args) {
		System.out.printf(fmt + (fmt.endsWith("\n") ? "" : "\n"), args);
	}

	public static Tpm remoteTpm(String hostname, int port) {
		return TpmFactory.remoteTpm(hostname, port);
	}

	public static byte[] rsaDecrypt(Tpm tpm, TPM_HANDLE keyHandle, byte[] cipher, byte[] label) {
		final byte[] plain = tpm.RSA_Decrypt(keyHandle, cipher, new TPMS_NULL_ASYM_SCHEME(), label);
		return plain;
	}

	public static byte[] rsaEncrypt(Tpm tpm, TPM_HANDLE keyHandle, byte[] plain, byte[] label) {
		final byte[] cipher = tpm.RSA_Encrypt(keyHandle, plain, new TPMS_NULL_ASYM_SCHEME(), label);
		return cipher;
	}

	public static void runProvisioningSequence(Tpm tpm) {
		try {
			if (CmdLine.isOptionPresent("clear", "c")) {
				print("Clearing keys ...");
				clearPersistent(tpm, EK_PersHandle, "EK");
				clearPersistent(tpm, SRK_PersHandle, "SRK");
				clearPersistent(tpm, ID_KEY_PersHandle, "ID");
				return;
			}
			TPMT_PUBLIC ekPub = null, srkPub = null;
			//
			// Make sure that device keys used in activation protocol exist
			//
			ekPub = createPersistentPrimary(tpm, EK_PersHandle, TPM_RH.ENDORSEMENT, EK_Template, "EK");
			srkPub = createPersistentPrimary(tpm, SRK_PersHandle, TPM_RH.OWNER, SRK_Template, "SRK");
			final int blobBufCapacity = 4096;
			final byte[] actBlobBuffer = new byte[blobBufCapacity];
			//
			// Obtain activation blob from the server
			//
			int actBlobSize = 0;
			final byte[] ekBlobToSend = (new TPM2B_PUBLIC(ekPub)).toBytes(),
					srkBlobToSend = (new TPM2B_PUBLIC(srkPub)).toBytes();
			// Initial version of the DRS protocol expected only key bytes (without complete
			// template data)
			// byte[] ekUnique = ((TPM2B_PUBLIC_KEY_RSA)ekPub.unique).buffer,
			// srkUnique = ((TPM2B_PUBLIC_KEY_RSA)srkPub.unique).buffer;
			actBlobSize = getActivationBlob(tpm, ekBlobToSend, ekBlobToSend.length, srkBlobToSend, srkBlobToSend.length,
					actBlobBuffer, blobBufCapacity);
			if (actBlobSize <= 0) {
				throw new Exception("Unexpected DRS failure");
			}
			//
			// Unmarshal components of the activation blob generated by DRS
			//
			final TpmBuffer actBlob = new TpmBuffer(Arrays.copyOfRange(actBlobBuffer, 0, actBlobSize));
			final TPM2B_ID_OBJECT credBlob = TPM2B_ID_OBJECT.fromTpm(actBlob);
			print("credBlob end: %d", actBlob.curPos());
			final TPM2B_ENCRYPTED_SECRET encSecret = TPM2B_ENCRYPTED_SECRET.fromTpm(actBlob);
			print("encSecret end: %d", actBlob.curPos());
			final TPM2B_PRIVATE idKeyDupBlob = TPM2B_PRIVATE.fromTpm(actBlob);
			print("idKeyDupBlob end: %d", actBlob.curPos());
			final TPM2B_ENCRYPTED_SECRET encWrapKey = TPM2B_ENCRYPTED_SECRET.fromTpm(actBlob);
			print("encWrapKey end: %d", actBlob.curPos());
			final TPM2B_PUBLIC idKeyPub = TPM2B_PUBLIC.fromTpm(actBlob);
			print("idKeyPub end: %d", actBlob.curPos());
			final TPM2B_DATA encUriData = TPM2B_DATA.fromTpm(actBlob);
			print("encUriData end: %d", actBlob.curPos());
			// Start a policy session to be used with ActivateCredential()
			final StartAuthSessionResponse sasResp = tpm.StartAuthSession(TPM_HANDLE.NULL, TPM_HANDLE.NULL,
					Helpers.RandomBytes(20), new byte[0], TPM_SE.POLICY,
					new TPMT_SYM_DEF(TPM_ALG_ID.NULL, 0, TPM_ALG_ID.NULL), TPM_ALG_ID.SHA256);
			// Apply the policy necessary to authorize an EK on Windows
			tpm.PolicySecret(TPM_HANDLE.from(TPM_RH.ENDORSEMENT), sasResp.handle, new byte[0], new byte[0], new byte[0],
					0);
			// Use ActivateCredential() to decrypt symmetric key that is used as an inner
			// protector
			// of the duplication blob of the new Device ID key generated by DRS.
			final byte[] innerWrapKey = tpm._withSessions(TPM_HANDLE.pwSession(new byte[0]), sasResp.handle)
					.ActivateCredential(SRK_PersHandle, EK_PersHandle, credBlob.credential, encSecret.secret);
			// Initialize parameters of the symmetric key used by DRS
			// Note that the client uses the key size chosen by DRS, but other parameters
			// are fixes (an AES key in CFB mode).
			final TPMT_SYM_DEF_OBJECT symDef = new TPMT_SYM_DEF_OBJECT(TPM_ALG_ID.AES, innerWrapKey.length * 8,
					TPM_ALG_ID.CFB);
			//
			// Import the new Device ID key issued by DRS into the device's TPM
			//
			final TPM2B_PRIVATE idKeyPriv = tpm.Import(SRK_PersHandle, innerWrapKey, idKeyPub.publicArea, idKeyDupBlob,
					encWrapKey.secret, symDef);
			//
			// Load and persist new Device ID key issued by DRS
			//
			final TPM_HANDLE hIdkey = tpm.Load(SRK_PersHandle, idKeyPriv, idKeyPub.publicArea);
			clearPersistent(tpm, ID_KEY_PersHandle, "ID Key");
			tpm.EvictControl(TPM_HANDLE.from(TPM_RH.OWNER), hIdkey, ID_KEY_PersHandle);
			print("Successfully created persistent %s 0x%08X\r\n", "ID Key", ID_KEY_PersHandle.handle);
			tpm.FlushContext(hIdkey);
			//
			// Decrypt URI data using TPM.
			// A recommended alternative for the actual SDK code is to use the symmetric
			// algorithm from a software crypto library
			//
			final int maxUriDataSize = TpmHelpers.getTpmProperty(tpm, TPM_PT.INPUT_BUFFER);
			if (encUriData.buffer.length > maxUriDataSize) {
				throw new Exception("Too long encrypted URI data string. Max supported length is "
						+ Integer.toString(maxUriDataSize));
			}
			// The template of the symmetric key used by the DRS
			final TPMT_PUBLIC symTemplate = new TPMT_PUBLIC(
					// TPMI_ALG_HASH nameAlg
					TPM_ALG_ID.SHA256,
					// TPMA_OBJECT objectAttributes
					new TPMA_OBJECT(TPMA_OBJECT.decrypt, TPMA_OBJECT.fixedTPM, TPMA_OBJECT.fixedParent,
							TPMA_OBJECT.userWithAuth),
					// TPM2B_DIGEST authPolicy
					new byte[0],
					// TPMU_PUBLIC_PARMS parameters
					new TPMS_SYMCIPHER_PARMS(symDef),
					// TPMU_PUBLIC_ID unique
					new TPM2B_DIGEST());
			// URI data are encrypted with the same symmetric key used as the inner
			// protector of the new Device ID key duplication blob.
			final TPMS_SENSITIVE_CREATE sensCreate = new TPMS_SENSITIVE_CREATE(new byte[0], innerWrapKey);
			final CreateResponse crResp = tpm.Create(SRK_PersHandle, sensCreate, symTemplate, new byte[0],
					new TPMS_PCR_SELECTION[0]);
			final TPM_HANDLE hSymKey = tpm.Load(SRK_PersHandle, crResp.outPrivate, crResp.outPublic);
			final byte[] iv = new byte[innerWrapKey.length];
			final EncryptDecryptResponse edResp = tpm.EncryptDecrypt(hSymKey, (byte) 1, TPM_ALG_ID.CFB, iv,
					encUriData.buffer);
			print("Decrypted URI data size: %d", edResp.outData.length);
			print("Decrypted URI [for native]: %s", new String(edResp.outData, Charset.forName("UTF-8")));
			print("Decrypted URI [for java]: %s", new String(edResp.outData));
			tpm.FlushContext(hSymKey);
			//
			// Generate token data, and sign it using the new Device ID key
			// (Note that this sample simply generates a random buffer in lieu of a valid
			// token)
			//
			final byte[] deviceIdData = Helpers.RandomBytes(2550);
			final byte[] signature = signData(tpm, idKeyPub.publicArea, deviceIdData);
			// Use DRS emulator library to make sure that the signature is correct
			// Note that the actual SDK does not need a code like this.
			final int rc = DrsServer.VerifyIdSignature(tpm, deviceIdData, signature);
			if (rc != TPM_RC.SUCCESS.toInt()) {
				throw new Exception("Failed to verify a signature created by the new Device ID key");
			}
			print("Successfully verified a signature created by the new Device ID key");
		} catch (final TpmException te) {
			final String rcName = te.ResponseCode == null ? "<NONE>" : te.ResponseCode.name();
			final String msg = te.getMessage();
			print("A TPM operations FAILED: error {%s}; message \"%s\"", rcName, msg);
		} catch (final Exception e) {
			print("An operation FAILED: Error message: \"%s\"", e.getMessage());
		}
		print("RunProvisioningSequence finished!");
	}

	public static void setAuth(Tpm tpm, byte[] newAuth) {
		final TPM_HANDLE platformHandle = TPM_HANDLE.from(TPM_RH.PLATFORM);
		tpm.Clear(platformHandle);
		tpm.HierarchyChangeAuth(platformHandle, newAuth);
		platformHandle.AuthValue = newAuth;
		tpm.Clear(platformHandle);
	}

	// NOTE: For now only HMAC signing is supported.
	public static byte[] signData(Tpm tpm, TPMT_PUBLIC idKeyPub, byte[] tokenData) {
		final TPM_ALG_ID idKeyHashAlg = ((TPMS_SCHEME_HMAC) ((TPMS_KEYEDHASH_PARMS) idKeyPub.parameters).scheme).hashAlg;
		final int MaxInputBuffer = TpmHelpers.getTpmProperty(tpm, TPM_PT.INPUT_BUFFER);

		if (tokenData.length <= MaxInputBuffer) {
			return tpm.HMAC(ID_KEY_PersHandle, tokenData, idKeyHashAlg);
		}

		int curPos = 0;
		int bytesLeft = tokenData.length;

		final TPM_HANDLE hSeq = tpm.HMAC_Start(ID_KEY_PersHandle, new byte[0], idKeyHashAlg);

		do {
			tpm.SequenceUpdate(hSeq, Arrays.copyOfRange(tokenData, curPos, curPos + MaxInputBuffer));

			bytesLeft -= MaxInputBuffer;
			curPos += MaxInputBuffer;
		} while (bytesLeft > MaxInputBuffer);

		return tpm.SequenceComplete(hSeq, Arrays.copyOfRange(tokenData, curPos, curPos + bytesLeft),
				TPM_HANDLE.from(TPM_RH.NULL)).result;
	}

	/* === FIRMA ========================================================== */
	public static byte[] signRsa(Tpm tpm, TPM_HANDLE keyHandle, byte[] digest, TPM_ALG_ID hashAlg) {
		final TPMS_SIG_SCHEME_RSASSA scheme = new TPMS_SIG_SCHEME_RSASSA(hashAlg);
		final TPMU_SIGNATURE sig = tpm.Sign(keyHandle, digest, scheme, null);
		return ((TPMS_SIGNATURE_RSASSA) sig).sig;
	}

	public static int verifyIdSignature(Tpm tpm, byte[] data, byte[] sig) {
		final byte[] hmac = Crypto.hmac(TPM_ALG_ID.SHA256, IdKeySens.data, data);
		return (Helpers.arraysAreEqual(sig, hmac) ? TPM_RC.SUCCESS : TPM_RC.SIGNATURE).toInt();
	}

	private TpmHelper() {
		// solo uso statico
	}

}