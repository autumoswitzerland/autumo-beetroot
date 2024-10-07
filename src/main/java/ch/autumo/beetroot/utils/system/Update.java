/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.autumo.beetroot.utils.system;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.utils.Helper;
import ch.autumo.beetroot.utils.common.Colors;


/**
 * Update Check.
 */
public class Update {

	private static final String HEAD;
    private static final String GITHUB_API_URL;
    private static final String GITHUB_REL_URL;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	static {
		try {
			BeetRootConfigurationManager.getInstance().initialize();
		} catch (Exception e) {
			System.err.println(Colors.red("Couldn't initialize configuration 'cfg/beetroot.cfg' !"));
			System.err.println(Colors.red("ERROR") + ": " + e.getMessage());
			e.printStackTrace();
			Helper.fatalExit();
		}		
		HEAD =
				"" 								+ LINE_SEPARATOR +
				"" 								+ LINE_SEPARATOR +
				Colors.darkCyan("Update Check")	+ LINE_SEPARATOR +
				"------------"	 				+ LINE_SEPARATOR;
		GITHUB_API_URL = BeetRootConfigurationManager.getInstance().getString("github_api_url", "https://api.github.com/repos/autumoswitzerland/autumo-beetroot/releases/latest");
		GITHUB_REL_URL = BeetRootConfigurationManager.getInstance().getString("github_rel_url", "https://github.com/autumoswitzerland/autumo-beetroot/releases");
	}
    
    /**
     * Fetches the latest release version from the GitHub repository.
     *
     * @return the latest release version as a string
     * @throws Exception if an error occurs during the request
     */
    public static String getLatestRelease() throws Exception {
        // Create HTTP client
        HttpClient client = HttpClient.newHttpClient();

        // Create the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(GITHUB_API_URL))
                .header("Accept", "application/vnd.github.v3+json")  // Set GitHub API version
                .GET()
                .build();

        // Send the request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Check if the request was successful
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch latest release. HTTP status: " + response.statusCode());
        }

        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(response.body());

        // Get the 'tag_name' field, which usually holds the release version
        return jsonResponse.getString("tag_name");
    }
    
    /**
     * Checks if the latest version is newer than the current version.
     * 
     * @param latestRelease the latest release version (e.g., "1.2.0")
     * @param currentReleaseVersion the current release version (e.g., "1.0.0")
     * @return true if latestRelease is newer, false otherwise
     */
    public static boolean isNewerVersion2(String latestRelease, String currentReleaseVersion) {
        // Split versions into major, minor, and patch numbers
        final String latestParts[] = latestRelease.split("\\.");
        final String currentParts[] = currentReleaseVersion.split("\\.");
        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latest = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            if (latest > current) {
                return true;  // A newer version is available
            } else if (latest < current) {
                return false; // Current version is newer
            }
        }
        // Versions are equal
        return false;
    }
    
    public static int isNewerVersion(String latestRelease, String currentReleaseVersion) {
        // Remove leading 'v' if present
    	latestRelease = latestRelease.startsWith("v") ? latestRelease.substring(1) : latestRelease;
    	currentReleaseVersion = currentReleaseVersion.startsWith("v") ? currentReleaseVersion.substring(1) : currentReleaseVersion;
        
        // Split versions into major, minor, and patch
        String[] v1Parts = latestRelease.split("\\.");
        String[] v2Parts = currentReleaseVersion.split("\\.");
        
        // Compare major, minor, and patch numbers
        for (int i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1 != v2) {
                return v1 - v2;  // If different, return comparison result
            }
        }
        
        return 0;  // If versions are equal
    }
    
    public static void main(String args[]) {
        try {
            //String currentReleaseVersion = "v2.0.1";
            String currentReleaseVersion = "v" + BeetRootConfigurationManager.getAppVersion();
            
            System.out.println(HEAD);
            
            System.out.println("Current release: " + Colors.cyan(currentReleaseVersion));
            String latestRelease = getLatestRelease();
            System.out.println("Latest release: " + Colors.cyan(latestRelease));
            System.out.println("");
            
            int res = isNewerVersion(latestRelease, currentReleaseVersion);
            
            if (res  > 0) {
                System.out.println("A new release is available: " + Colors.green(latestRelease));
                System.out.println("Download new release here: " + Colors.green(GITHUB_REL_URL));
            } else if (res < 0) {
                System.out.println("You are using a newer version " + Colors.darkYellow(currentReleaseVersion) + " than the latest release!");
            } else {
                System.out.println(Colors.green("You are using the latest release."));
            }
        } catch (Exception e) {
			System.err.println(Colors.red("ERROR") + ": " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("");
    }
    
}
