/*
* This file is part of the GAIA software.
* Copyright 2011 University of Maryland
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package linqs.gaia.graph.dbgraph;

import java.util.List;

import linqs.gaia.graph.SystemDataManager;
import linqs.gaia.identifiable.ID;
import linqs.gaia.log.Log;

/**
 * Store system data into two tables.
 * 
 * @author namatag
 *
 */
public abstract class DBSystemManager extends DBHelper implements SystemDataManager {
	private static String kv = "gaia_sdkv";
	private static String dkv = "gaia_sddkv";
	
	public void initializeSystemManager() {
		Log.DEBUG("Initializing system data table");
		this.dropTableIfExists(kv);
		this.executeSQL("CREATE TABLE "+kv+" (sdkey varchar(255), sdvalue varchar(255), PRIMARY KEY (sdkey))");
		this.dropTableIfExists(dkv);
		this.executeSQL("CREATE TABLE "+dkv+" (dkey varchar(255), sdkey varchar(255), sdvalue varchar(255), PRIMARY KEY (dkey, sdkey))");
	}
	
	public void removeAllSystemData() {
		this.truncateTable(kv);
		this.truncateTable(dkv);
	}

	public void removeSystemData(ID id) {
		this.executeSQL("DELETE FROM "+dkv+" WHERE dkey='"+id.toString()+"'");	
	}

	public String getSystemData(String key) {
		String value = null;
		
		List<String> values = 
			this.queryDatabaseString("SELECT sdvalue FROM "+kv+" WHERE sdkey='"+key+"'","sdvalue");
		
		if(values!=null && !values.isEmpty()) {
			value = values.get(0);
		}
		
		return value;
	}

	public String getSystemData(ID id, String key) {
		String value = null;
		
		List<String> values = 
			this.queryDatabaseString("SELECT sdvalue FROM "+dkv
					+" WHERE dkey='"+id.toString()+"' AND sdkey='"+key+"'", "sdvalue");
		
		if(values!=null && !values.isEmpty()) {
			value = values.get(0);
		}
		
		return value;
	}

	public void removeSystemData(String key) {
		this.executeSQL("DELETE FROM "+kv+" WHERE sdkey='"+key+"'");	
	}

	public void removeSystemData(ID id, String key) {
		this.executeSQL("DELETE FROM "+dkv+" WHERE dkey='"+id.toString()+"' AND sdkey='"+key+"'");
	}

	public void setSystemData(String key, String value) {
		this.executeSQL("INSERT INTO "+kv+" (sdkey,sdvalue) VALUES ('"+key+"','"+value+"')");
	}

	public void setSystemData(ID id, String key, String value) {
		this.executeSQL("INSERT INTO "+dkv+" (dkey,sdkey,sdvalue)" +
				" VALUES ('"+id.toString()+"','"+key+"','"+value+"')");
	}
}
