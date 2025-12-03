package com.project.sportssync;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final String PRIMARY_FILE = "attendance_primary.xml";
    private static final String RECOVERY_FILE = "attendance_recovery.xml";
    private static final String SPORTS_CACHE_FILE = "sports_cache.json";

    private Context context;

    public BackupManager(Context context) {
        this.context = context;
    }

    public void saveAttendance(AttendanceRecord record) {
        List<AttendanceRecord> currentList = getAttendanceHistory();
        currentList.add(record);
        writeToXml(currentList, PRIMARY_FILE);
        // Also update recovery immediately for safety
        writeToXml(currentList, RECOVERY_FILE);
    }

    public void saveAttendanceList(List<AttendanceRecord> records) {
        writeToXml(records, PRIMARY_FILE);
        writeToXml(records, RECOVERY_FILE);
    }

    public void backupToRecovery() {
        List<AttendanceRecord> currentList = getAttendanceHistory();
        writeToXml(currentList, RECOVERY_FILE);
    }

    public boolean restoreFromRecovery() {
        File recovery = new File(context.getFilesDir(), RECOVERY_FILE);
        if (!recovery.exists()) {
            return false;
        }
        List<AttendanceRecord> recoveredList = readFromXml(RECOVERY_FILE);
        if (recoveredList != null && !recoveredList.isEmpty()) {
            writeToXml(recoveredList, PRIMARY_FILE);
            return true;
        }
        return false;
    }

    public List<AttendanceRecord> getAttendanceHistory() {
        return readFromXml(PRIMARY_FILE);
    }

    private void writeToXml(List<AttendanceRecord> records, String fileName) {
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("", "attendance_list");

            for (AttendanceRecord record : records) {
                serializer.startTag("", "record");
                
                serializer.startTag("", "uucms");
                serializer.text(record.getUucms() != null ? record.getUucms() : "");
                serializer.endTag("", "uucms");

                serializer.startTag("", "name");
                serializer.text(record.getStudentName() != null ? record.getStudentName() : "");
                serializer.endTag("", "name");

                serializer.startTag("", "sport");
                serializer.text(record.getSport() != null ? record.getSport() : "");
                serializer.endTag("", "sport");

                serializer.startTag("", "status");
                serializer.text(record.getStatus() != null ? record.getStatus() : "");
                serializer.endTag("", "status");

                serializer.startTag("", "timestamp");
                serializer.text(record.getTimestamp() != null ? record.getTimestamp() : "");
                serializer.endTag("", "timestamp");

                serializer.endTag("", "record");
            }

            serializer.endTag("", "attendance_list");
            serializer.endDocument();
            
            fos.write(writer.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error writing XML", e);
        }
    }

    private List<AttendanceRecord> readFromXml(String fileName) {
        List<AttendanceRecord> records = new ArrayList<>();
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            return records;
        }

        try {
            FileInputStream fis = context.openFileInput(fileName);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new InputStreamReader(fis));

            int eventType = parser.getEventType();
            AttendanceRecord currentRecord = null;
            String currentTag = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.getName();
                    if ("record".equals(currentTag)) {
                        currentRecord = new AttendanceRecord();
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    String text = parser.getText();
                    if (currentRecord != null && text != null) {
                        switch (currentTag) {
                            case "uucms":
                                currentRecord.setUucms(text);
                                break;
                            case "name":
                                currentRecord.setStudentName(text);
                                break;
                            case "sport":
                                currentRecord.setSport(text);
                                break;
                            case "status":
                                currentRecord.setStatus(text);
                                break;
                            case "timestamp":
                                currentRecord.setTimestamp(text);
                                break;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("record".equals(parser.getName()) && currentRecord != null) {
                        records.add(currentRecord);
                        currentRecord = null;
                    }
                    currentTag = "";
                }
                eventType = parser.next();
            }
            fis.close();
        } catch (Exception e) {
            Log.e(TAG, "Error reading XML", e);
        }
        return records;
    }

    // Sports Cache Methods
    public void saveSportsCache(List<SportModel> sports) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SportModel sport : sports) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", sport.getId());
                jsonObject.put("name", sport.getName());
                
                // Serialize equipment map
                JSONObject equipmentJson = new JSONObject();
                if (sport.getEquipment() != null) {
                    for (String key : sport.getEquipment().keySet()) {
                        SportModel.EquipmentItem item = sport.getEquipment().get(key);
                        JSONObject itemJson = new JSONObject();
                        itemJson.put("name", item.getName());
                        itemJson.put("totalQuantity", item.getTotalQuantity());
                        itemJson.put("availableQuantity", item.getAvailableQuantity());
                        equipmentJson.put(key, itemJson);
                    }
                }
                jsonObject.put("equipment", equipmentJson);
                
                jsonArray.put(jsonObject);
            }

            FileOutputStream fos = context.openFileOutput(SPORTS_CACHE_FILE, Context.MODE_PRIVATE);
            fos.write(jsonArray.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving sports cache", e);
        }
    }

    public List<SportModel> getSportsCache() {
        List<SportModel> sports = new ArrayList<>();
        File file = new File(context.getFilesDir(), SPORTS_CACHE_FILE);
        if (!file.exists()) {
            return null; // Return null to indicate no cache
        }

        try {
            FileInputStream fis = context.openFileInput(SPORTS_CACHE_FILE);
            InputStreamReader isr = new InputStreamReader(fis);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = isr.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            fis.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SportModel sport = new SportModel();
                sport.setId(jsonObject.optString("id"));
                sport.setName(jsonObject.optString("name"));

                // Deserialize equipment
                JSONObject equipmentJson = jsonObject.optJSONObject("equipment");
                if (equipmentJson != null) {
                    java.util.Map<String, SportModel.EquipmentItem> equipmentMap = new java.util.HashMap<>();
                    java.util.Iterator<String> keys = equipmentJson.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        JSONObject itemJson = equipmentJson.getJSONObject(key);
                        SportModel.EquipmentItem item = new SportModel.EquipmentItem();
                        item.setName(itemJson.optString("name"));
                        item.setTotalQuantity(itemJson.optInt("totalQuantity"));
                        item.setAvailableQuantity(itemJson.optInt("availableQuantity"));
                        equipmentMap.put(key, item);
                    }
                    sport.setEquipment(equipmentMap);
                }
                sports.add(sport);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading sports cache", e);
            return null;
        }
        return sports;
    }
}
