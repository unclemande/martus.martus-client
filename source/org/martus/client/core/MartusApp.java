/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2004, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/

package org.martus.client.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import org.martus.client.core.BulletinStore.BulletinAlreadyExistsException;
import org.martus.client.core.ClientSideNetworkHandlerUsingXmlRpc.SSLSocketSetupException;
import org.martus.client.core.Exceptions.ServerCallFailedException;
import org.martus.client.core.Exceptions.ServerNotAvailableException;
import org.martus.client.search.BulletinSearcher;
import org.martus.client.search.SearchParser;
import org.martus.client.search.SearchTreeNode;
import org.martus.common.CustomFields;
import org.martus.common.FieldSpec;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.LegacyCustomFields;
import org.martus.common.MartusConstants;
import org.martus.common.MartusUtilities;
import org.martus.common.ProgressMeterInterface;
import org.martus.common.StandardFieldSpecs;
import org.martus.common.Version;
import org.martus.common.CustomFields.CustomFieldsParseException;
import org.martus.common.HQKeys.HQsException;
import org.martus.common.MartusUtilities.FileVerificationException;
import org.martus.common.MartusUtilities.PublicInformationInvalidException;
import org.martus.common.MartusUtilities.ServerErrorException;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.clientside.CurrentUiState;
import org.martus.common.clientside.DateUtilities;
import org.martus.common.clientside.Localization;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.crypto.MartusCrypto.CryptoException;
import org.martus.common.crypto.MartusCrypto.MartusSignatureException;
import org.martus.common.database.FileDatabase.MissingAccountMapException;
import org.martus.common.database.FileDatabase.MissingAccountMapSignatureException;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceForNonSSL;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.FieldDataPacket;
import org.martus.common.packet.UniversalId;
import org.martus.common.packet.Packet.WrongAccountException;
import org.martus.util.Base64;
import org.martus.util.ByteArrayInputStreamWithSeek;
import org.martus.util.DirectoryUtils;
import org.martus.util.FileInputStreamWithSeek;
import org.martus.util.InputStreamWithSeek;
import org.martus.util.UnicodeReader;
import org.martus.util.UnicodeWriter;
import org.martus.util.Base64.InvalidBase64Exception;


public class MartusApp
{
	
	public MartusApp(Localization localizationToUse) throws MartusAppInitializationException
	{
		this(null, determineMartusDataRootDirectory(), localizationToUse);
	}

	public MartusApp(MartusCrypto cryptoToUse, File dataDirectoryToUse, Localization localizationToUse) throws MartusAppInitializationException
	{
		localization = localizationToUse;
		try
		{
			if(cryptoToUse == null)
				cryptoToUse = new MartusSecurity();

			configInfo = new ConfigInfo();
			currentUserName = "";
			maxNewFolders = MAXFOLDERS;
			martusDataRootDirectory = dataDirectoryToUse;
			store = new BulletinStore(cryptoToUse);
		}
		catch(MartusCrypto.CryptoInitializationException e)
		{
			throw new MartusAppInitializationException("ErrorCryptoInitialization");
		}

		initializeCurrentLanguage();
	}

	private void initializeCurrentLanguage()
	{
		CurrentUiState previouslySavedState = new CurrentUiState();
		previouslySavedState.load(getUiStateFile());

		if(previouslySavedState.getCurrentLanguage() != "")
		{	
			localization.setCurrentLanguageCode(previouslySavedState.getCurrentLanguage());
			localization.setCurrentDateFormatCode(previouslySavedState.getCurrentDateFormat());
		}
		
		if(localization.getCurrentLanguageCode()== null)
			setInitialUiDefaultsFromFileIfPresent(localization, new File(getMartusDataRootDirectory(),"DefaultUi.txt"));
		
		if(localization.getCurrentLanguageCode()== null)
		{
			localization.setCurrentLanguageCode(Localization.ENGLISH);
			localization.setCurrentDateFormatCode(DateUtilities.getDefaultDateFormatCode());
		}
	}
	
	static public void setInitialUiDefaultsFromFileIfPresent(Localization localization, File defaultUiFile)
	{
		if(!defaultUiFile.exists())
			return;
		try
		{
			String languageCode = null;
			UnicodeReader in = new UnicodeReader(defaultUiFile);
			languageCode = in.readLine();
			in.close();
			
			if(Localization.isRecognizedLanguage(languageCode))
			{
				localization.setCurrentLanguageCode(languageCode);
				localization.setCurrentDateFormatCode(Localization.getDefaultDateFormatForLanguage(languageCode));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setServerInfo(String serverName, String serverKey, String serverCompliance)
	{
		configInfo.setServerName(serverName);
		configInfo.setServerPublicKey(serverKey);
		configInfo.setServerCompliance(serverCompliance);
		try
		{
			saveConfigInfo();
		}
		catch (SaveConfigInfoException e)
		{
			System.out.println("MartusApp.setServerInfo: Unable to Save ConfigInfo" + e);
		}

		invalidateCurrentHandlerAndGateway();
	}

	public String getLegacyHQKey()
	{
		return configInfo.getLegacyHQKey();
	}
	
	public HQKeys getHQKeys() throws HQsException
	{
		return new HQKeys(configInfo.getAllHQKeysXml());
	}

	public HQKeys getHQKeysWithFallback()
	{
		try
		{
			return getHQKeys();
		}
		catch (HQsException e)
		{
			e.printStackTrace();
			HQKey legacyKey = new HQKey(getLegacyHQKey());
			return new HQKeys(legacyKey);
		}
	}
	
	public String getHQLabelIfPresent(String hqKey)
	{
		try
		{
			return getHQKeys().getLabelIfPresent(hqKey);
		}
		catch (HQsException e)
		{
			e.printStackTrace();
			return "";
		}
	}

	public ConfigInfo getConfigInfo()
	{
		return configInfo;
	}
	
	public void setAndSaveHQKeys(HQKeys hQKeys) throws SaveConfigInfoException 
	{
		configInfo.setAllHQKeysXml(hQKeys.toStringWithLabel());
		if(hQKeys.isEmpty())
			configInfo.clearHQKey();
		else
			configInfo.setLegacyHQKey(hQKeys.get(0).getPublicKey());
		saveConfigInfo();
	}

	public void saveConfigInfo() throws SaveConfigInfoException
	{
		try
		{
			ByteArrayOutputStream encryptedConfigOutputStream = new ByteArrayOutputStream();
			configInfo.save(encryptedConfigOutputStream);
			byte[] encryptedConfigInfo = encryptedConfigOutputStream.toByteArray();

			ByteArrayInputStream encryptedConfigInputStream = new ByteArrayInputStream(encryptedConfigInfo);
			FileOutputStream configFileOutputStream = new FileOutputStream(getConfigInfoFile());
			getSecurity().encrypt(encryptedConfigInputStream, configFileOutputStream);

			configFileOutputStream.close();
			encryptedConfigInputStream.close();
			encryptedConfigOutputStream.close();


			FileInputStream in = new FileInputStream(getConfigInfoFile());
			byte[] signature = getSecurity().createSignatureOfStream(in);
			in.close();

			FileOutputStream out = new FileOutputStream(getConfigInfoSignatureFile());
			out.write(signature);
			out.close();
		}
		catch (Exception e)
		{
			System.out.println("saveConfigInfo :" + e);
			throw new SaveConfigInfoException();
		}

	}

	public void loadConfigInfo() throws LoadConfigInfoException
	{
		configInfo.clear();

		File sigFile = getConfigInfoSignatureFile();
		File dataFile = getConfigInfoFile();

		if(!dataFile.exists())
		{
			//System.out.println("MartusApp.loadConfigInfo: config file doesn't exist");
			return;
		}

		try
		{
			byte[] signature =	new byte[(int)sigFile.length()];
			FileInputStream inSignature = new FileInputStream(sigFile);
			inSignature.read(signature);
			inSignature.close();

			FileInputStream inData = new FileInputStream(dataFile);
			boolean verified = getSecurity().isValidSignatureOfStream(getSecurity().getPublicKeyString(), inData, signature);
			inData.close();
			if(!verified)
				throw new LoadConfigInfoException();

			InputStreamWithSeek encryptedContactFileInputStream = new FileInputStreamWithSeek(dataFile);
			ByteArrayOutputStream plainTextContactOutputStream = new ByteArrayOutputStream();
			getSecurity().decrypt(encryptedContactFileInputStream, plainTextContactOutputStream);

			byte[] plainTextConfigInfo = plainTextContactOutputStream.toByteArray();
			ByteArrayInputStream plainTextConfigInputStream = new ByteArrayInputStream(plainTextConfigInfo);
			configInfo = ConfigInfo.load(plainTextConfigInputStream);

			plainTextConfigInputStream.close();
			plainTextContactOutputStream.close();
			encryptedContactFileInputStream.close();
			
			FieldSpec[] specs = getCustomFieldSpecs(configInfo);
			store.setPublicFieldTags(specs);
			
			convertLegacyHQToMultipleHQs();
			
		}
		catch (Exception e)
		{
			//System.out.println("Loadcontactinfo: " + e);
			throw new LoadConfigInfoException();
		}
	}
	
	private void convertLegacyHQToMultipleHQs() throws HQsException
	{
		String legacyHQKey = configInfo.getLegacyHQKey();
		if(legacyHQKey.length()>0)
		{
			HQKeys hqKeys = getHQKeys();
			if(!hqKeys.containsKey(legacyHQKey))
			{
				HQKey legacy = new HQKey(legacyHQKey);
				hqKeys.add(legacy);
				try
				{
					setAndSaveHQKeys(hqKeys);
				}
				catch(MartusApp.SaveConfigInfoException e)
				{
					System.out.println("SaveConfigInfoException: " + e);						
				}
			}
		}
	}

	public static FieldSpec[] getCustomFieldSpecs(ConfigInfo configInfo) throws CustomFieldsParseException
	{
		String xmlSpecs = configInfo.getCustomFieldXml();
		if(xmlSpecs.length() > 0)
			return CustomFields.parseXml(xmlSpecs);
			
		String legacySpecs = configInfo.getCustomFieldSpecs();
		FieldSpec[] specs = LegacyCustomFields.parseFieldSpecsFromString(legacySpecs);
		return specs;
	}

	public void doAfterSigninInitalization() throws MartusAppInitializationException, FileVerificationException, MissingAccountMapException, MissingAccountMapSignatureException
	{
		store.doAfterSigninInitialization(getCurrentAccountDirectory());
	}
	
	public File getMartusDataRootDirectory()
	{
		return martusDataRootDirectory;
	}

	public File getCurrentAccountDirectory()
	{
		return currentAccountDirectory;
	}
	
	public File getPacketsDirectory()
	{
		return new File(getCurrentAccountDirectory(), PACKETS_DIRECTORY_NAME);
	}
	
	public File getAccountsDirectory()
	{
		return new File(getMartusDataRootDirectory(), ACCOUNTS_DIRECTORY_NAME);
		
	}

	public String getCurrentAccountDirectoryName()
	{
		return getCurrentAccountDirectory().getPath() + "/";
	}

	public File getConfigInfoFile()
	{
		return getConfigInfoFileForAccount(getCurrentAccountDirectory());
	}
	
	public File getConfigInfoFileForAccount(File accountDirectory)
	{
		return new File(accountDirectory, "MartusConfig.dat");
	}

	public File getConfigInfoSignatureFile()
	{
		return getConfigInfoSignatureFileForAccount(getCurrentAccountDirectory());
	}

	public File getConfigInfoSignatureFileForAccount(File accountDirectory)
	{
		return new File(accountDirectory, "MartusConfig.sig");
	}

	public File getUploadInfoFile()
	{
		return getUploadInfoFileForAccount(getCurrentAccountDirectory());
	}

	public File getUploadInfoFileForAccount(File accountDirectory)
	{
		return new File(accountDirectory, "MartusUploadInfo.dat");
	}

	public File getUiStateFile()
	{
		if(isSignedIn())
			return getUiStateFileForAccount(getCurrentAccountDirectory());
		return new File(getMartusDataRootDirectory(), "UiState.dat");
	}

	public File getUiStateFileForAccount(File accountDirectory)
	{
		return new File(accountDirectory, "UserUiState.dat");
	}
	
	public File getBulletinDefaultDetailsFile()
	{
		return new File(getCurrentAccountDirectoryName(), "DefaultDetails" + DEFAULT_DETAILS_EXTENSION);
	}

	public String getUploadLogFilename()
	{
		return  getCurrentAccountDirectoryName() + "MartusUploadLog.txt";
	}

	public String getHelpFilename(String languageCode)
	{
		String helpFile = "MartusHelp-" + languageCode + ".txt";
		return helpFile;
	}

	public String getEnglishHelpFilename()
	{
		return("MartusHelp-en.txt");
	}

	public String getHelpTOCFilename(String languageCode)
	{
		String helpFile = "MartusHelpTOC-" + languageCode + ".txt";
		return helpFile;
	}

	public static File getTranslationsDirectory()
	{
		return determineMartusDataRootDirectory();
	}

	public File getCurrentKeyPairFile()
	{
		File dir = getCurrentAccountDirectory();
		return getKeyPairFile(dir);
	}

	public File getKeyPairFile(File dir)
	{
		return new File(dir, KEYPAIR_FILENAME);
	}	

	public static File getBackupFile(File original)
	{
		return new File(original.getPath() + ".bak");
	}
	
	public String getUserName()
	{
		return currentUserName;
	}

	public void loadFolders()
	{
		store.loadFolders();
	}

	public BulletinStore getStore()
	{
		return store;
	}

	public Bulletin createBulletin()
	{
		Bulletin b = store.createEmptyBulletin();
		b.set(Bulletin.TAGAUTHOR, configInfo.getAuthor());
		b.set(Bulletin.TAGORGANIZATION, configInfo.getOrganization());
		b.set(Bulletin.TAGPUBLICINFO, configInfo.getTemplateDetails());
		b.set(Bulletin.TAGLANGUAGE, getCurrentLanguage());
		b.setDraft();
		b.setAllPrivate(true);
		return b;
	}

	public void setHQKeysInBulletin(Bulletin b)
	{
		HQKeys hqKeys = getHQKeysWithFallback();
		b.setAuthorizedToReadKeys(hqKeys);
	}

	public BulletinFolder getFolderSaved()
	{
		return store.getFolderSaved();
	}

	public BulletinFolder getFolderDiscarded()
	{
		return store.getFolderDiscarded();
	}

	public BulletinFolder getFolderSealedOutbox()
	{
		return store.getFolderSealedOutbox();
	}

	public BulletinFolder getFolderDraftOutbox()
	{
		return store.getFolderDraftOutbox();
	}

	public BulletinFolder createFolderRetrieved()
	{
		String folderName = getNameOfFolderRetrievedSealed();
		return createOrFindFolder(folderName);
	}

	public BulletinFolder createFolderRetrievedFieldOffice()
	{
		String folderName = getNameOfFolderRetrievedFieldOfficeSealed();
		return createOrFindFolder(folderName);
	}

	public String getNameOfFolderRetrievedSealed()
	{
		return store.getNameOfFolderRetrievedSealed();
	}

	public String getNameOfFolderRetrievedDraft()
	{
		return store.getNameOfFolderRetrievedDraft();
	}

	public String getNameOfFolderRetrievedFieldOfficeSealed()
	{
		return store.getNameOfFolderRetrievedFieldOfficeSealed();
	}

	public String getNameOfFolderRetrievedFieldOfficeDraft()
	{
		return store.getNameOfFolderRetrievedFieldOfficeDraft();
	}

	public BulletinFolder createOrFindFolder(String name)
	{
		return store.createOrFindFolder(name);
	}

	public void setMaxNewFolders(int numFolders)
	{
		maxNewFolders = numFolders;
	}

	public BulletinFolder createUniqueFolder(String originalFolderName)
	{
		BulletinFolder newFolder = null;
		String uniqueFolderName = null;
		int folderIndex = 0;
		while (newFolder == null && folderIndex < maxNewFolders)
		{
			uniqueFolderName = originalFolderName;
			if (folderIndex > 0)
				uniqueFolderName += folderIndex;
			newFolder = store.createFolder(uniqueFolderName);
			++folderIndex;
		}
		if(newFolder != null)
			store.saveFolders();
		return newFolder;
	}
	
	public void cleanupWhenCompleteQuickErase()
	{
		store.deleteFoldersDatFile();	
	}
	
	public void deleteKeypairAndRelatedFilesForAccount(File accountDirectory)
	{
		File keyPairFile = getKeyPairFile(accountDirectory);
		DirectoryUtils.scrubAndDeleteFile(keyPairFile);
		DirectoryUtils.scrubAndDeleteFile(getBackupFile(keyPairFile));
		DirectoryUtils.scrubAndDeleteFile(getUserNameHashFile(keyPairFile.getParentFile()));
		DirectoryUtils.scrubAndDeleteFile(getConfigInfoFileForAccount(accountDirectory));
		DirectoryUtils.scrubAndDeleteFile(getConfigInfoSignatureFileForAccount(accountDirectory));
		DirectoryUtils.scrubAndDeleteFile(getUploadInfoFileForAccount(accountDirectory));
		DirectoryUtils.scrubAndDeleteFile(getUiStateFileForAccount(accountDirectory));
		DirectoryUtils.scrubAndDeleteFile(BulletinStore.getFoldersFileForAccount(accountDirectory));
		DirectoryUtils.scrubAndDeleteFile(BulletinStore.getCacheFileForAccount(accountDirectory));
		File[] exportedKeys = exportedPublicKeyFiles(accountDirectory);
		for (int i = 0; i < exportedKeys.length; i++)
		{
			File file = exportedKeys[i];
			DirectoryUtils.scrubAndDeleteFile(file);
		}
	}

	private static File[] exportedPublicKeyFiles(File accountDir)
	{
		File[] mpiFiles = accountDir.listFiles(new FileFilter()
		{
			public boolean accept(File file)
			{
				return (file.isFile() && file.getName().endsWith(".mpi"));	
			}
		});
		return mpiFiles;
	}

	public boolean deleteAllBulletinsAndUserFolders()
	{
		try
		{											
			store.scrubAllData();
			store.deleteAllData();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public int quarantineUnreadableBulletins()
	{
		return store.quarantineUnreadableBulletins();
	}

	public int repairOrphans()
	{
		Set orphans = store.getSetOfOrphanedBulletinUniversalIds();
		int foundOrphanCount = orphans.size();
		if(foundOrphanCount == 0)
			return 0;

		Iterator it = orphans.iterator();
		while(it.hasNext())
		{
			UniversalId uid = (UniversalId)it.next();
			try
			{
				store.addRepairBulletinToFolders(uid);
			}
			catch (BulletinAlreadyExistsException e)
			{
				System.out.println("Orphan Bulletin already exists.");
			}
			catch (IOException shouldNeverHappen)
			{
				shouldNeverHappen.printStackTrace();
			}
		}

		store.saveFolders();
		return foundOrphanCount;
	}


	public Vector findBulletinInAllVisibleFolders(Bulletin b)
	{
		return store.findBulletinInAllVisibleFolders(b);
	}

	public boolean isDraftOutboxEmpty()
	{
		if(getFolderDraftOutbox().getBulletinCount() == 0)
			return true;
		return false;
	}

	public boolean isSealedOutboxEmpty()
	{
		if(getFolderSealedOutbox().getBulletinCount() == 0)
			return true;
		return false;
	}
	
	public void discardBulletinsFromFolder(BulletinFolder folderToDiscardFrom, Bulletin[] bulletinsToDiscard) throws IOException 
	{
		for (int i = 0; i < bulletinsToDiscard.length; i++)
		{
			Bulletin b = bulletinsToDiscard[i];
			getStore().discardBulletin(folderToDiscardFrom, b);
		}
		getStore().saveFolders();
	}

	public Date getUploadInfoElement(int index)
	{
		File file = getUploadInfoFile();
		if (!file.canRead())
			return null;
		Date date = null;
		try
		{
			ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
			for(int i = 0 ; i < index ; ++i)
			{
				stream.readObject();
			}
			date = (Date)stream.readObject();
			stream.close();
		}
		catch (Exception e)
		{
			System.out.println("Error reading from getUploadInfoElement " + index + ":" + e);
		}
		return date;

	}

	public Date getLastUploadedTime()
	{
		return(getUploadInfoElement(0));
	}

	public Date getLastUploadRemindedTime()
	{
		return(getUploadInfoElement(1));
	}


	public void setUploadInfoElements(Date uploaded, Date reminded)
	{
		File file = getUploadInfoFile();
		file.delete();
		try
		{
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
			stream.writeObject(uploaded);
			stream.writeObject(reminded);
			stream.close();
		}
		catch (Exception e)
		{
			System.out.println("Error writing to setUploadInfoElements:" + e);
		}

	}

	public void setLastUploadedTime(Date uploaded)
	{
		Date reminded = getLastUploadRemindedTime();
		setUploadInfoElements(uploaded, reminded);
	}

	public void setLastUploadRemindedTime(Date reminded)
	{
		Date uploaded = getLastUploadedTime();
		setUploadInfoElements(uploaded, reminded);
	}

	public void resetLastUploadedTime()
	{
		setLastUploadedTime(new Date());
	}

	public void resetLastUploadRemindedTime()
	{
		setLastUploadRemindedTime(new Date());
	}

	public void search(String searchFor, String startDate, String endDate, String andKeyword, String orKeyword)
	{
		SearchParser parser = new SearchParser(andKeyword, orKeyword);
		SearchTreeNode searchNode = parser.parse(searchFor);
		BulletinSearcher matcher = new BulletinSearcher(searchNode, startDate, endDate);

		BulletinFolder searchFolder = createOrFindFolder(store.getSearchFolderName());
		searchFolder.removeAll();

		Vector uids = store.getAllBulletinUids();
		for(int i = 0; i < uids.size(); ++i)
		{
			UniversalId uid = (UniversalId)uids.get(i);
			Bulletin b = store.findBulletinByUniversalId(uid);
			if(matcher.doesMatch(b))
			{	
				try
				{
					store.addBulletinToFolder(searchFolder, b.getUniversalId());
				}
				catch (BulletinAlreadyExistsException safeToIgnoreException)
				{
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		store.saveFolders();
	}

	public boolean isNonSSLServerAvailable(String serverName)
	{
		if(!isServerConfigured())
			return false;

		NetworkInterfaceForNonSSL server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverName);
		return isNonSSLServerAvailable(server);
	}

	public boolean isSSLServerAvailable()
	{
		if(currentNetworkInterfaceHandler == null && !isServerConfigured())
			return false;

		return isSSLServerAvailable(getCurrentNetworkInterfaceGateway());
	}
	
	public boolean isServerConfigured()
	{
		return (getServerName().length() > 0);
	}

	public ClientSideNetworkGateway buildGateway(String serverName, String serverPublicKey)
	{
		NetworkInterface server = buildNetworkInterface(serverName, serverPublicKey);
		if(server == null)
			return null;
		
		return new ClientSideNetworkGateway(server);
	}

	NetworkInterface buildNetworkInterface(String serverName, String serverPublicKey)
	{
		if(serverName.length() == 0)
			return null;
	
		try
		{
			int[] ports = NetworkInterfaceXmlRpcConstants.defaultSSLPorts;
			ClientSideNetworkHandlerUsingXmlRpc handler = new ClientSideNetworkHandlerUsingXmlRpc(serverName, ports);
			handler.getSimpleX509TrustManager().setExpectedPublicKey(serverPublicKey);
			return handler;
		}
		catch (SSLSocketSetupException e)
		{
			//TODO propagate to UI and needs a test.
			e.printStackTrace();
			return null;
		}
	}

	public boolean isSignedIn()
	{
		return getSecurity().hasKeyPair();
	}

	public String getServerPublicCode(String serverName) throws
		ServerNotAvailableException,
		PublicInformationInvalidException
	{
		try
		{
			return MartusCrypto.computePublicCode(getServerPublicKey(serverName));
		}
		catch(Base64.InvalidBase64Exception e)
		{
			throw new PublicInformationInvalidException();
		}
	}

	public String getServerPublicKey(String serverName) throws
		ServerNotAvailableException,
		PublicInformationInvalidException
	{
		NetworkInterfaceForNonSSL server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverName);
		return getServerPublicKey(server);
	}

	public String getServerPublicKey(NetworkInterfaceForNonSSL server) throws
		ServerNotAvailableException,
		PublicInformationInvalidException
	{
		if(server.ping() == null)
			throw new ServerNotAvailableException();

		Vector serverInformation = server.getServerInformation();
		if(serverInformation == null)
			throw new ServerNotAvailableException();

		if(serverInformation.size() != 3)
			throw new PublicInformationInvalidException();

		String accountId = (String)serverInformation.get(1);
		String sig = (String)serverInformation.get(2);
		MartusUtilities.validatePublicInfo(accountId, sig, getSecurity());
		return accountId;
	}

	public boolean requestServerUploadRights(String magicWord)
	{
		try
		{
			NetworkResponse response = getCurrentNetworkInterfaceGateway().getUploadRights(getSecurity(), magicWord);
			if(response.getResultCode().equals(NetworkInterfaceConstants.OK))
				return true;
		}
		catch(MartusCrypto.MartusSignatureException e)
		{
			System.out.println("MartusApp.requestServerUploadRights: " + e);
		}

		return false;
	}

	public Vector getNewsFromServer()
	{
		if(!isSSLServerAvailable())
			return new Vector();

		try
		{
			NetworkResponse response = getCurrentNetworkInterfaceGateway().getNews(getSecurity());
			if(response.getResultCode().equals(NetworkInterfaceConstants.OK))
				return response.getResultVector();
		}
		catch (MartusSignatureException e)
		{
			System.out.println("MartusApp.getNewsFromServer :" + e);
		}
		return new Vector();
	}

	public String getServerCompliance(ClientSideNetworkGateway gateway) 
		throws ServerCallFailedException, ServerNotAvailableException
	{
		if(!isSSLServerAvailable(gateway))
			throw new ServerNotAvailableException();
		try
		{
			NetworkResponse response = gateway.getServerCompliance(getSecurity());
			if(response.getResultCode().equals(NetworkInterfaceConstants.OK))
				return (String)response.getResultVector().get(0);
		}
		catch (Exception e)
		{
			//System.out.println("MartusApp.getServerCompliance :" + e);
			throw new ServerCallFailedException();
		}		
		throw new ServerCallFailedException();
	}

	void moveBulletinToDamaged(BulletinFolder outbox, UniversalId uid)
	{
		System.out.println("Moving bulletin to damaged");
		BulletinFolder damaged = createOrFindFolder(store.getNameOfFolderDamaged());
		Bulletin b = store.findBulletinByUniversalId(uid);
		store.moveBulletin(b, outbox, damaged);		
	}

	public static class DamagedBulletinException extends Exception
	{
		public DamagedBulletinException(String message)
		{
			super(message);
		}
	}

	public Vector getMyServerBulletinSummaries() throws ServerErrorException
	{
		if(!isSSLServerAvailable())
			throw new ServerErrorException("No server");

		String resultCode = "?";
		try
		{
			NetworkResponse response = getCurrentNetworkInterfaceGateway().getSealedBulletinIds(getSecurity(), getAccountId(), MartusUtilities.getRetrieveBulletinSummaryTags());
			resultCode = response.getResultCode();
			if(resultCode.equals(NetworkInterfaceConstants.OK))
				return response.getResultVector();
		}
		catch (MartusSignatureException e)
		{
			System.out.println("MartusApp.getMyServerBulletinSummaries: " + e);
			resultCode = NetworkInterfaceConstants.SIG_ERROR;
		}
		throw new ServerErrorException(resultCode);
	}

	public Vector getMyDraftServerBulletinSummaries() throws ServerErrorException
	{
		if(!isSSLServerAvailable())
			throw new ServerErrorException("No server");

		String resultCode = "?";
		try
		{
			NetworkResponse response = getCurrentNetworkInterfaceGateway().getDraftBulletinIds(getSecurity(), getAccountId(), MartusUtilities.getRetrieveBulletinSummaryTags());
			resultCode = response.getResultCode();
			if(resultCode.equals(NetworkInterfaceConstants.OK))
				return response.getResultVector();
		}
		catch (MartusSignatureException e)
		{
			System.out.println("MartusApp.getMyDraftServerBulletinSummaries: " + e);
			resultCode = NetworkInterfaceConstants.SIG_ERROR;
		}
		throw new ServerErrorException(resultCode);
	}

	public Vector downloadFieldOfficeAccountIds() throws ServerErrorException
	{
		if(!isSSLServerAvailable())
			throw new ServerErrorException();

		try
		{
			NetworkResponse response = getCurrentNetworkInterfaceGateway().getFieldOfficeAccountIds(getSecurity(), getAccountId());
			String resultCode = response.getResultCode();
			if(!resultCode.equals(NetworkInterfaceConstants.OK))
				throw new ServerErrorException(resultCode);
			return response.getResultVector();
		}
		catch(MartusCrypto.MartusSignatureException e)
		{
			System.out.println("MartusApp.getFieldOfficeAccounts: " + e);
			throw new ServerErrorException();
		}
	}

	public FieldDataPacket retrieveFieldDataPacketFromServer(String authorAccountId, String bulletinLocalId, String dataPacketLocalId) throws Exception
	{
		NetworkResponse response = getCurrentNetworkInterfaceGateway().getPacket(getSecurity(), authorAccountId, bulletinLocalId, dataPacketLocalId);
		String resultCode = response.getResultCode();
		if(!resultCode.equals(NetworkInterfaceConstants.OK))
			throw new ServerErrorException(resultCode);

		String xmlencoded = (String)response.getResultVector().get(0);
		String xml = new String(Base64.decode(xmlencoded), "UTF-8");
		UniversalId uid = UniversalId.createFromAccountAndLocalId(authorAccountId, dataPacketLocalId);
		FieldDataPacket fdp = new FieldDataPacket(uid , StandardFieldSpecs.getDefaultPublicFieldSpecs());
		byte[] xmlBytes = xml.getBytes("UTF-8");
		ByteArrayInputStreamWithSeek in =  new ByteArrayInputStreamWithSeek(xmlBytes);
		fdp.loadFromXml(in, getSecurity());
		return fdp;
	}

	public BulletinSummary retrieveSummaryFromString(String accountId, String parameters)
		throws ServerErrorException
	{
		FieldDataPacket fdp = null;
		String args[] = parameters.split(MartusConstants.regexEqualsDelimeter, -1);
		if(args.length < 3)
			throw new ServerErrorException("MartusApp.retrieveSummaryFromString invalid # params: " + parameters);
		String bulletinLocalId= args[0];
		String packetlocalId = args[1];
		int size = Integer.parseInt(args[2]);
		String date = "";
		if(args.length > 3)
			date = args[3];
	
		if(!FieldDataPacket.isValidLocalId(packetlocalId))
			throw new ServerErrorException();
	
		UniversalId uId = UniversalId.createFromAccountAndLocalId(accountId, bulletinLocalId);
		Bulletin bulletin = store.findBulletinByUniversalId(uId);
		if (bulletin != null)
			fdp = bulletin.getFieldDataPacket();
	
		try
		{
			if(fdp == null)
				fdp = retrieveFieldDataPacketFromServer(accountId, bulletinLocalId, packetlocalId);
		}
		catch(Exception e)
		{
			//System.out.println("MartusApp.retrieveSummaryFromString Exception: bulletinLocalId=" + bulletinLocalId + " packetlocalId=" + packetlocalId );
			//e.printStackTrace();
			throw new ServerErrorException();
		}
		BulletinSummary bulletinSummary = new BulletinSummary(accountId, bulletinLocalId, fdp, size, date);
		return bulletinSummary;
}

	public void retrieveOneBulletinToFolder(UniversalId uid, BulletinFolder retrievedFolder, ProgressMeterInterface progressMeter) throws
		Exception
	{
		File tempFile = File.createTempFile("$$$MartusApp", null);
		tempFile.deleteOnExit();
		FileOutputStream outputStream = new FileOutputStream(tempFile);

		int masterTotalSize = BulletinZipUtilities.retrieveBulletinZipToStream(uid, outputStream,
						serverChunkSize, getCurrentNetworkInterfaceGateway(),  getSecurity(),
						progressMeter);

		outputStream.close();

		if(tempFile.length() != masterTotalSize)
			throw new ServerErrorException("totalSize didn't match data length");

		store.importZipFileBulletin(tempFile, retrievedFolder, true);
		tempFile.delete();
		
		Bulletin b = store.findBulletinByUniversalId(uid);
		store.setIsOnServer(b);
	}

	public String deleteServerDraftBulletins(Vector uidList) throws
		MartusSignatureException,
		WrongAccountException
	{
		String[] localIds = new String[uidList.size()];
		for (int i = 0; i < localIds.length; i++)
		{
			UniversalId uid = (UniversalId)uidList.get(i);
			if(!uid.getAccountId().equals(getAccountId()))
				throw new WrongAccountException();

			localIds[i] = uid.getLocalId();
		}
		NetworkResponse response = getCurrentNetworkInterfaceGateway().deleteServerDraftBulletins(getSecurity(), getAccountId(), localIds);
		return response.getResultCode();
	}

	public static class AccountAlreadyExistsException extends Exception {}
	public static class CannotCreateAccountFileException extends IOException {}

	public void createAccount(String userName, char[] userPassPhrase) throws
					Exception
	{
		if(doesAccountExist(userName, userPassPhrase))
			throw new AccountAlreadyExistsException();
		
		if(doesDefaultAccountExist())
			createAdditionalAccount(userName, userPassPhrase);
		else
			createAccountInternal(getMartusDataRootDirectory(), userName, userPassPhrase);
	}

	public boolean doesAccountExist(String userName, char[] userPassPhrase) throws Exception
	{
		return (getAccountDirectoryForUser(userName, userPassPhrase) != null);
	}

	public File getAccountDirectoryForUser(String userName, char[] userPassPhrase) throws Exception
	{
		Vector allAccountDirs = getAllAccountDirectories();
		MartusSecurity tempSecurity = new MartusSecurity();
		for(int i = 0; i<allAccountDirs.size(); ++i )
		{
			File testAccountDirectory = (File)allAccountDirs.get(i);
			if(isUserOwnerOfThisAccountDirectory(tempSecurity, userName, userPassPhrase, testAccountDirectory))
				return testAccountDirectory;
		}
		return null;
	}

	private void createAdditionalAccount(String userName, char[] userPassPhrase) throws Exception
	{
		File tempAccountDir = null;
		try
		{
			File accountsDirectory = getAccountsDirectory();
			accountsDirectory.mkdirs();
			tempAccountDir = File.createTempFile("temp", null, accountsDirectory);
			tempAccountDir.delete();
			tempAccountDir.mkdirs();
			createAccountInternal(tempAccountDir, userName, userPassPhrase);
			String realAccountDirName = getAccountDirectoryName(getAccountId());
			File realAccountDir = new File(accountsDirectory, realAccountDirName);

			if(tempAccountDir.renameTo(realAccountDir))
				setCurrentAccount(userName, realAccountDir);
			else
				System.out.println("createAdditionalAccount rename failed.");
		}
		catch (Exception e)
		{
			System.out.println("createAdditionalAccount failed.");
			DirectoryUtils.deleteEntireDirectoryTree(tempAccountDir);
			throw(e);
		}
	}

	public void createAccountInternal(File accountDataDirectory, String userName, char[] userPassPhrase) throws
		Exception
	{
		File keyPairFile = getKeyPairFile(accountDataDirectory);
		if(keyPairFile.exists())
			throw(new AccountAlreadyExistsException());
		getSecurity().clearKeyPair();
		getSecurity().createKeyPair();
		try
		{
			writeKeyPairFileWithBackup(keyPairFile, userName, userPassPhrase);
			attemptSignInInternal(keyPairFile, userName, userPassPhrase);
		}
		catch(Exception e)
		{
			getSecurity().clearKeyPair();
			throw(e);
		}
	}
	
	public Vector getAllAccountDirectories()
	{
		Vector accountDirectories = new Vector();
		accountDirectories.add(getMartusDataRootDirectory());
		File accountsDirectoryRoot = getAccountsDirectory();
		File[] contents = accountsDirectoryRoot.listFiles();
		if(contents== null)
			return accountDirectories;
		for (int i = 0; i < contents.length; i++)
		{
			File thisFile = contents[i];
			try
			{
				if(!thisFile.isDirectory())
				{	
					continue;
				}
				String name = thisFile.getName();
				if(name.length() != 24)
				{	
					continue;
				}
				if(MartusCrypto.removeNonDigits(name).length() != 20)
				{	
					continue;
				}
				accountDirectories.add(thisFile);
			}
			catch (Exception notAValidAccountDirectory)
			{
			}
		}
		return accountDirectories;
	}
	
	public File getAccountDirectory(String accountId) throws InvalidBase64Exception
	{
		String name = getAccountDirectoryName(accountId);
		File accountDir = new File(getAccountsDirectory(), name);
		if(accountDir.exists() && accountDir.isDirectory())
			return accountDir;
		if(!getKeyPairFile(getMartusDataRootDirectory()).exists())
			return getMartusDataRootDirectory();
		accountDir.mkdirs();
		return accountDir;
	}

	private String getAccountDirectoryName(String accountId)
		throws InvalidBase64Exception
	{
		return MartusCrypto.getFormattedPublicCode(accountId);
	}

	public boolean doesAnyAccountExist()
	{
		Vector accountDirectories = getAllAccountDirectories();
		for (int i = 0; i < accountDirectories.size(); i++)
		{
			File thisDirectory = (File)accountDirectories.get(i);
			if(getKeyPairFile(thisDirectory).exists())
				return true;
		}
		return false;
	}
	
	public boolean doesDefaultAccountExist()
	{
		if(getKeyPairFile(getMartusDataRootDirectory()).exists())
			return true;

		File packetsDir = new File(getMartusDataRootDirectory(), PACKETS_DIRECTORY_NAME);
		if(!packetsDir.exists())
			return false;

		return (packetsDir.listFiles().length > 0);
	}

	public void exportPublicInfo(File exportFile) throws
		IOException,
		Base64.InvalidBase64Exception,
		MartusCrypto.MartusSignatureException
	{
		MartusUtilities.exportClientPublicKey(getSecurity(), exportFile);
	}

	public String extractPublicInfo(File file) throws
		IOException,
		Base64.InvalidBase64Exception,
		PublicInformationInvalidException
	{
		Vector importedPublicKeyInfo = MartusUtilities.importClientPublicKeyFromFile(file);
		String publicKey = (String) importedPublicKeyInfo.get(0);
		String signature = (String) importedPublicKeyInfo.get(1);
		MartusUtilities.validatePublicInfo(publicKey, signature, getSecurity());
		return publicKey;
	}

	public File getPublicInfoFile(String fileName)
	{
		fileName = MartusUtilities.toFileName(fileName);
		String completeFileName = fileName + PUBLIC_INFO_EXTENSION;
		return(new File(getCurrentAccountDirectoryName(), completeFileName));
	}

	public void attemptSignIn(String userName, char[] userPassPhrase) throws Exception
	{
		File keyPairFile = getAccountDirectoryForUser(userName, userPassPhrase);
		attemptSignInInternal(getKeyPairFile(keyPairFile), userName, userPassPhrase);
	}
	
	public void attemptReSignIn(String userName, char[] userPassPhrase) throws Exception
	{
		attemptReSignInInternal(getCurrentKeyPairFile(), userName, userPassPhrase);
	}
	
	private String getCurrentLanguage()
	{
		return localization.getCurrentLanguageCode();
	}

	public String getAccountId()
	{
		return getSecurity().getPublicKeyString();
	}
	
	public void writeKeyPairFileWithBackup(File keyPairFile, String userName, char[] userPassPhrase) throws
		CannotCreateAccountFileException
	{
		writeKeyPairFileInternal(keyPairFile, userName, userPassPhrase);
		try
		{
			writeKeyPairFileInternal(getBackupFile(keyPairFile), userName, userPassPhrase);
		}
		catch (Exception e)
		{
			System.out.println("MartusApp.writeKeyPairFileWithBackup: " + e);
		}
	}

	protected void writeKeyPairFileInternal(File keyPairFile, String userName, char[] userPassPhrase) throws
		CannotCreateAccountFileException
	{
		try
		{
			FileOutputStream outputStream = new FileOutputStream(keyPairFile);
			try
			{
				getSecurity().writeKeyPair(outputStream, getCombinedPassPhrase(userName, userPassPhrase));
			}
			finally
			{
				outputStream.close();
			}
		}
		catch(IOException e)
		{
			throw(new CannotCreateAccountFileException());
		}

	}

	public void attemptSignInInternal(File keyPairFile, String userName, char[] userPassPhrase) throws Exception
	{
		try
		{
			getSecurity().readKeyPair(keyPairFile, getCombinedPassPhrase(userName, userPassPhrase));
			setCurrentAccount(userName, keyPairFile.getParentFile());
		}
		catch(Exception e)
		{
			getSecurity().clearKeyPair();
			currentUserName = "";
			throw e;
		}
	}
	
	public void attemptReSignInInternal(File keyPairFile, String userName, char[] userPassPhrase) throws Exception
	{
		if(!userName.equals(currentUserName))
			throw new MartusCrypto.AuthorizationFailedException();
		MartusCrypto securityOfReSignin = new MartusSecurity();
		FileInputStream inputStream = new FileInputStream(keyPairFile);
		try
		{
			securityOfReSignin.readKeyPair(inputStream, getCombinedPassPhrase(userName, userPassPhrase));
		}
		finally
		{
			inputStream.close();
		}
	}

	public void setCurrentAccount(String userName, File accountDirectory) throws IOException
	{
		currentUserName = userName;
		currentAccountDirectory = accountDirectory;
		updateUserNameHashFile();
	}

	private void updateUserNameHashFile() throws IOException
	{
		File hashUserName = getUserNameHashFile(currentAccountDirectory);
		hashUserName.delete();
		String hashOfUserName = MartusSecurity.getHexDigest(currentUserName);
		UnicodeWriter writer = new UnicodeWriter(hashUserName);
		try
		{
			writer.writeln(hashOfUserName);
		}
		finally
		{
			writer.close();
		}
	}
	
	public boolean isUserOwnerOfThisAccountDirectory(MartusSecurity tempSecurity, String userName, char[] userPassPhrase, File accountDirectory) throws IOException
	{
		File thisAccountsHashOfUserNameFile = getUserNameHashFile(accountDirectory);
		if(thisAccountsHashOfUserNameFile.exists())
		{
			UnicodeReader reader = new UnicodeReader(thisAccountsHashOfUserNameFile);
			try
			{
				String hashOfUserName = reader.readLine();
				String hexDigest = MartusSecurity.getHexDigest(userName);
				if(hashOfUserName.equals(hexDigest))
					return true;
			}
			finally
			{
				reader.close();
			}
			return false;
		}

		File thisAccountsKeyPair = getKeyPairFile(accountDirectory);
		try
		{
			tempSecurity.readKeyPair(thisAccountsKeyPair, getCombinedPassPhrase(userName, userPassPhrase));
			return true;
		}
		catch (Exception cantBeOurAccount)
		{
			return false;
		}
	}

	public File getUserNameHashFile(File accountDirectory)
	{
		return new File(accountDirectory, "AccountToken.txt");
	}

	public char[] getCombinedPassPhrase(String userName, char[] userPassPhrase)
	{
		char[] combined = new char[userName.length() + userPassPhrase.length + 1];
		System.arraycopy(userPassPhrase,0,combined,0,userPassPhrase.length);
		combined[userPassPhrase.length] = ':';
		System.arraycopy(userName.toCharArray(),0,combined,userPassPhrase.length+1,userName.length());
		
		return(combined);
	}

	public MartusCrypto getSecurity()
	{
		return store.getSignatureGenerator();
	}

	public void setSSLNetworkInterfaceHandlerForTesting(NetworkInterface server)
	{
		currentNetworkInterfaceHandler = server;
	}

	private boolean isNonSSLServerAvailable(NetworkInterfaceForNonSSL server)
	{
		String result = server.ping();
		if(result == null)
			return false;

		if(result.indexOf("MartusServer") != 0)
			return false;

		return true;
	}

	public boolean isSSLServerAvailable(ClientSideNetworkGateway server)
	{
		try
		{
			NetworkResponse response = server.getServerInfo();
			if(!response.getResultCode().equals(NetworkInterfaceConstants.OK))
				return false;

			String version = (String)response.getResultVector().get(0);
			if(version.indexOf("MartusServer") == 0)
				return true;
		}
		catch(Exception notInterestingBecauseTheServerMightJustBeDown)
		{
			//System.out.println("MartusApp.isSSLServerAvailable: " + e);
		}

		return false;
	}

	public ClientSideNetworkGateway getCurrentNetworkInterfaceGateway()
	{
		if(currentNetworkInterfaceGateway == null)
		{
			currentNetworkInterfaceGateway = new ClientSideNetworkGateway(getCurrentNetworkInterfaceHandler());
		}

		return currentNetworkInterfaceGateway;
	}

	private NetworkInterface getCurrentNetworkInterfaceHandler()
	{
		if(currentNetworkInterfaceHandler == null)
		{
			currentNetworkInterfaceHandler = createXmlRpcNetworkInterfaceHandler();
		}

		return currentNetworkInterfaceHandler;
	}

	private NetworkInterface createXmlRpcNetworkInterfaceHandler()
	{
		String ourServer = getServerName();
		String ourServerPublicKey = getConfigInfo().getServerPublicKey();
		return buildNetworkInterface(ourServer,ourServerPublicKey);
	}

	private void invalidateCurrentHandlerAndGateway()
	{
		currentNetworkInterfaceHandler = null;
		currentNetworkInterfaceGateway = null;
	}

	private String getServerName()
	{
		return configInfo.getServerName();
	}

	private static File determineMartusDataRootDirectory()
	{
		String dir;
		if(Version.isRunningUnderWindows())
		{
			dir = "C:/Martus/";
		}
		else
		{
			String userHomeDir = System.getProperty("user.home");
			dir = userHomeDir + "/.Martus/";
		}
		File file = new File(dir);
		if(!file.exists())
		{
			file.mkdirs();
		}

		return file;
	}

	public void saveBulletin(Bulletin bulletinToSave, BulletinFolder outboxToUse) throws IOException, CryptoException
	{
		store.saveBulletin(bulletinToSave);
		store.ensureBulletinIsInFolder(store.getFolderSaved(), bulletinToSave.getUniversalId());
		store.ensureBulletinIsInFolder(outboxToUse, bulletinToSave.getUniversalId());
		store.removeBulletinFromFolder(store.getFolderDiscarded(), bulletinToSave);
		store.setIsNotOnServer(bulletinToSave);
		store.saveFolders();
	}

	public class SaveConfigInfoException extends Exception {}
	public class LoadConfigInfoException extends Exception {}

	public static class MartusAppInitializationException extends Exception
	{
		MartusAppInitializationException(String message)
		{
			super(message);
		}
	}

	public File martusDataRootDirectory;
	protected File currentAccountDirectory;
	private Localization localization;
	public BulletinStore store;
	private ConfigInfo configInfo;
	public NetworkInterface currentNetworkInterfaceHandler;
	public ClientSideNetworkGateway currentNetworkInterfaceGateway;
	public String currentUserName;
	private int maxNewFolders;

	public static final String PUBLIC_INFO_EXTENSION = ".mpi";
	public static final String DEFAULT_DETAILS_EXTENSION = ".txt";
	public static final String AUTHENTICATE_SERVER_FAILED = "Failed to Authenticate Server";
	public static final String SHARE_KEYPAIR_FILENAME_EXTENSION = ".dat";
	public static final String KEYPAIR_FILENAME = "MartusKeyPair.dat";
	public static final String ACCOUNTS_DIRECTORY_NAME = "accounts";
	public static final String PACKETS_DIRECTORY_NAME = "packets";
	
	private final int MAXFOLDERS = 50;
	public int serverChunkSize = NetworkInterfaceConstants.MAX_CHUNK_SIZE;
}

