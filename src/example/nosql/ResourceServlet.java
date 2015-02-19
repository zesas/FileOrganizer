package example.nosql;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import com.cloudant.client.api.Database;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;



@RestController
@RequestMapping("/favorites")
@Scope(value = "singleton")
/**
 * CRUD service of favorites application. It uses REST style.
 *
 */
public class ResourceServlet {
	
		
	
	@RequestMapping(value="/attach", method = RequestMethod.POST)
	public ResponseEntity<String> UploadFile(@RequestParam("file") MultipartFile fileParts, @RequestParam("id")  Long id, @RequestParam("name")  String name, @RequestParam("value")  String value) throws Exception {

		byte[] bytes = null;
		JsonObject resultObject = new JsonObject();
		String fileName = null;
		String contentType = null;
		
		 if (!fileParts.isEmpty()) {
	            try {
	                bytes = fileParts.getBytes();
	                contentType = fileParts.getContentType();
	                fileName = fileParts.getOriginalFilename();	              
	            }
	            catch (Exception e) {
	            	return new ResponseEntity<String>("File upload failed " + name + " => " + e.getMessage(), HttpStatus.NOT_FOUND);
	            }
		 }
		
		
		InputStream fileInputStream = new ByteArrayInputStream(bytes);
		
		Database db = null;
		try
		{
			db = getDB();
		}
		catch(Exception re)
		{
			return new ResponseEntity<String>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		//check if document exist
		HashMap<String, Object> obj = (id==-1?null:db.find(HashMap.class , id+""));

		if(obj==null)
		{ // if new document
			
			id = System.currentTimeMillis();
			
			//create a new document
			System.out.println("Creating new document with id : "+id);
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("name", name);			
			data.put("_id", id+"");
			data.put("value", value);
			data.put("creation_date", new Date().toString());
			db.save(data);	
			
			//attach the attachment object
			obj = db.find(HashMap.class , id+"");
			db.saveAttachment(fileInputStream, fileName, contentType, id+"",  (String)obj.get("_rev"));
		}
		else
		{ // if existing document
			//attach the attachment object
			db.saveAttachment(fileInputStream, fileName, contentType, id+"",  (String)obj.get("_rev"));
			
			//update other fields in the document
			obj = db.find(HashMap.class , id+"");
			obj.put("name", name);
			obj.put("value", value);
			db.update(obj);
			
		}	
		
		
		fileInputStream.close();
		
						
		System.out.println("Upload completed....");
		
		//get attachments
		obj = db.find(HashMap.class , id+"");		
		LinkedTreeMap<String, Object> attachments = (LinkedTreeMap<String, Object>)obj.get("_attachments");
		
		if(attachments!=null && attachments.size()>0)
		{
			JsonArray attachmentList = getAttachmentList(attachments, id+"");
			resultObject.add("attachements", attachmentList);
		}
		resultObject.addProperty("id", id);
		resultObject.addProperty("name", name);	
		resultObject.addProperty("value", value);
		
		return new ResponseEntity<String>(resultObject.toString(), HttpStatus.OK);
	}

			
    @RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<String> create(@RequestParam("name")  String name, @RequestParam("value")  String value) throws Exception{
		
		System.out.println("Create invoked...");
		Database db = null;
		try
		{
			db = getDB();
		}
		catch(Exception re)
		{
			return new ResponseEntity<String>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		JsonObject resultObject = new JsonObject();
		
		Map<String, Object> data = new HashMap<String, Object>();
		long id = System.currentTimeMillis();
		data.put("name", name);		
		data.put("_id", id+"");
		data.put("value", value);
		data.put("creation_date", new Date().toString());
		
		db.save(data);
		
		HashMap<String, Object> obj = db.find(HashMap.class, id+"");
		
		System.out.println("Create Successful...");
		resultObject.addProperty("id", id);
		resultObject.addProperty("name", name);	
		resultObject.addProperty("value", value);
		
		return new ResponseEntity<String>(resultObject.toString(), HttpStatus.OK);
	}
	
    @RequestMapping(method = RequestMethod.DELETE)
	public ResponseEntity<String> delete(@RequestParam("id")  Long id) throws Exception{
		boolean recordFound = true;
		Database db = null;
		try
		{
			db = getDB();
		}
		catch(Exception re)
		{
			return new ResponseEntity<String>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		//check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class , id+"");
		
		if(obj==null)
			recordFound = false;
		else
		db.remove(obj);
		System.out.println("Delete Successful...");
		
		
		if(recordFound){			
			return new ResponseEntity<String>(HttpStatus.OK);
		} else
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
	}
	
    @RequestMapping(method = RequestMethod.PUT)
	public ResponseEntity<String> update(@RequestParam("id")  long id, @RequestParam("name")  String name, @RequestParam("value")  String value) throws Exception{
		boolean recordFound = true;
		Database db = null;
		try
		{
			db = getDB();
		}
		catch(Exception re)
		{
			return new ResponseEntity<String>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		//check if document exist
		HashMap<String, Object> obj = db.find(HashMap.class , id+"");
		
		if(obj==null)
			recordFound = false;
		else
		{
			obj.put("name", name);
			obj.put("value", value);
		}
		
		db.update(obj);
		System.out.println("Update Successful...");
		
				
			
		if(recordFound){			
			return new ResponseEntity<String>(HttpStatus.OK);
		} else
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<String> get(@RequestParam(value = "id", required = false)  Long id, @RequestParam(value = "cmd", required = false) String cmd ) throws Exception {
		
		Database db = null;
		try
		{
			db = getDB();
		}
		catch(Exception re)
		{
			return new ResponseEntity<String>(re.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		JsonObject resultObject = new JsonObject();
		JsonArray jsonArray = new JsonArray();		
			
		if( id == null ){			
			try
			{
				//get all the document present in database
				List<HashMap> allDocs = db.view("_all_docs").query(HashMap.class); 
				
				if(allDocs.size()==0)
				{
					allDocs = initializeSampleData(db);
				}
				
				for(HashMap doc : allDocs)
				{
					
					HashMap<String, Object> obj = db.find(HashMap.class, doc.get("id")+"");
					JsonObject jsonObject = new JsonObject();					
					LinkedTreeMap<String, Object> attachments = (LinkedTreeMap<String, Object>) obj.get("_attachments");		
										
					if(attachments!=null && attachments.size()>0)
					{	
						JsonArray attachmentList = getAttachmentList(attachments, obj.get("_id")+"");
						jsonObject.addProperty("id", obj.get("_id")+"");
						jsonObject.addProperty("name", obj.get("name")+"");
						jsonObject.addProperty("value", obj.get("value")+"");
						jsonObject.add("attachements", attachmentList);
						
					}
					else
					{
						jsonObject.addProperty("id", obj.get("_id")+"");
						jsonObject.addProperty("name", obj.get("name")+"");
						jsonObject.addProperty("value", obj.get("value")+"");
					}
					
					jsonArray.add(jsonObject);
				}
			
			}
			catch(Exception dnfe)
			{
				System.out.println("Exception thrown : "+ dnfe.getMessage());
			}
			
			resultObject.addProperty("id", "all");
			resultObject.add("body", jsonArray);			
		
			return new ResponseEntity<String>(resultObject.toString(),HttpStatus.OK);
						
		}
		
		
		//check if document exists
		HashMap<String, Object> obj = db.find(HashMap.class , id+"");
				
				
		if(obj!=null)
		{
			JsonObject jsonObject = new JsonObject();			
			LinkedTreeMap<String, Object> attachments = (LinkedTreeMap<String, Object>)obj.get("_attachments");
			
			if(attachments!=null && attachments.size()>0)
			{
				JsonArray attachmentList = getAttachmentList(attachments, obj.get("_id")+"");
				jsonObject.add("attachements", attachmentList);
			}
			jsonObject.addProperty("id", obj.get("_id")+"");
			jsonObject.addProperty("name", obj.get("name")+"");
			jsonObject.addProperty("value", obj.get("value")+"");
			return new ResponseEntity<String>(jsonObject.toString(),HttpStatus.OK);
			
		}
		else
			return new ResponseEntity<String>(HttpStatus.NOT_FOUND);
			
				
	}
	
	private JsonArray getAttachmentList(LinkedTreeMap<String, Object> attachmentList, String docID) throws Exception
	{
		
		JsonArray attachmentArray = new JsonArray();
		String URLTemplate = "http://"+CloudantClientMgr.getUser()+":"+CloudantClientMgr.getPassword()+"@"+CloudantClientMgr.getHost()+"/"+CloudantClientMgr.getDatabaseName()+"/";
		
		for(Object key : attachmentList.keySet())
		{
			LinkedTreeMap<String, Object> attach = (LinkedTreeMap<String, Object>)attachmentList.get(key);	
			
			JsonObject attachedObject = new JsonObject();
			//set the content type of the attachment
			attachedObject.addProperty("content_type", attach.get("content_type").toString());
			//append the document id and attachment key to the URL
			attachedObject.addProperty("url", URLTemplate+docID+"/"+key);
			//set the key of the attachment
			attachedObject.addProperty("key", key+"");
			
			//add the attachment object to the array
			attachmentArray.add(attachedObject);
		}
		
		return attachmentArray;
		
	}
	
	/*
	 * Create a document and Initialize with sample data/attachments
	 */
	private List<HashMap> initializeSampleData(Database db) throws Exception
	{
				
		long id = System.currentTimeMillis();
		String name = "Sample category";;
		String value = "List of sample files";
		
		//create a new document
		System.out.println("Creating new document with id : "+id);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("name", name);			
		data.put("_id", id+"");
		data.put("value", value);
		data.put("creation_date", new Date().toString());
		db.save(data);	
		
		//attach the object
		HashMap<String, Object> obj = db.find(HashMap.class , id+"");		
		
		//attachment#1
		File file = new File("Sample.txt");
		file.createNewFile();		
		PrintWriter writer = new PrintWriter(file);
		writer.write("This is a sample file...");
		writer.flush();
		writer.close();
		FileInputStream fileInputStream = new FileInputStream(file);		
		db.saveAttachment(fileInputStream, file.getName(), "text/plain", id+"", (String)obj.get("_rev"));
		fileInputStream.close();
		
		List<HashMap> allDocs = db.view("_all_docs").query(HashMap.class); 
		return allDocs;
			
	}
	
	private Database getDB()
	{
		return CloudantClientMgr.getDB();
	}
	
}
