/*
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.glidein.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;

public class CredentialUtil
{
	public static void store(GlobusCredential credential, File file) 
	throws IOException
	{
		// Create the file if it doesn't exist
		if (!file.exists() && !file.createNewFile()) {
			throw new IOException("Unable to create credential file");
		}
		
		// Change file permissions
		FilesystemUtil.chmod(file, 600);
		
		// Save the credential to the file
		FileOutputStream pstream = 
			new FileOutputStream(file);
		credential.save(pstream);
		pstream.close();
	}
	
	public static GlobusCredential load(File file) 
	throws IOException
	{
		try {
			FileInputStream fis = new FileInputStream(file);
			GlobusCredential cred = new GlobusCredential(fis);
			fis.close();
			return cred;
		} catch (GlobusCredentialException gce) {
			IOException ioe = new IOException("Invalid credential");
			ioe.initCause(gce);
			throw ioe;
		}
	}
}
