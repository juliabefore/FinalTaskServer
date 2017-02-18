package com.miskevich.core;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class DataBaseService {

    private static final String PERSONS = "persons";
    private static final String PHONES = "phones";
    private static final String PATH = "src" + File.separator + "main" + File.separator + "java" + File.separator
            + "com" + File.separator + "miskevich" + File.separator + "data" + File.separator;
    private static final String DB_FILE_SUFFIX = ".xlsx";

    private Socket socket;

    public DataBaseService(Socket socket){
        this.socket = socket;
    }

    private enum Method{
        INSERT("INSERT"), SELECT("SELECT");

        private String method;

        Method(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }
    }


    public void save(String query) throws ServerException, IOException {
        String tableName = getTableNameInsert(query);
        queryValidations(tableName, query, Method.INSERT);

        List<String> columnValuesFromQuery = getColumnValuesFromInsertQuery(query);
        Map<String, String> mapFromQueryColNameToColValue = createMapFromQueryColNameToColValue(getColumnNamesFromInsertQuery(query), columnValuesFromQuery);
        uniqueConstraintCheck(tableName, mapFromQueryColNameToColValue);

        insertDataIntoTable(mapFromQueryColNameToColValue, tableName, PATH);

        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bufferedWriter.write("Object was saved in the file " + tableName);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        System.out.println("Server finished with response...");
    }

    public void getAll(String query) throws ServerException, IOException {
        String tableName = getTableNameSelect(query);
        queryValidations(tableName, query, Method.SELECT);

        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        List<Map<String, String>> allValues = getAllValues(query);
        for (Map<String, String> valuesForObject : allValues) {

            for (Map.Entry<String, String> entry : valuesForObject.entrySet()){
                bufferedWriter.write(entry.toString());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        }
        System.out.println("Server finished with response...");
    }

    public void getById(String query) throws ServerException, IOException {
        String tableName = getTableNameSelect(query);
        queryValidations(tableName, query, Method.SELECT);

        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        Map<String, String> valuesFromDBById = getValuesById(query);
        for (Map.Entry<String, String> entry : valuesFromDBById.entrySet()){
            bufferedWriter.write(entry.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        }
        System.out.println("Server finished with response...");
    }

    private void queryValidations(String tableName, String query, Method method){
        checkIfTableFromQueryExistsInDB(getTableList(PATH), tableName);
        List<String> columnNamesFromTable = getColumnNamesFromTable(tableName, PATH);
        List<String> columnNamesFromQuery = null;
        if(method.equals(Method.INSERT)){
            columnNamesFromQuery = getColumnNamesFromInsertQuery(query);
        }else if(method.equals(Method.SELECT)){
            columnNamesFromQuery = getColumnNamesFromSelectQuery(query);
        }
        areColumnsValid(columnNamesFromQuery, columnNamesFromTable, tableName);
    }

    static String getTableNameInsert(String query){
        return query.substring(12, query.indexOf("("));
    }

    static String getTableNameSelect(String query){
        String tableName;
        String tableStart = query.substring(query.indexOf("FROM") + 5);
        if(tableStart.contains(" ")){
            tableName = tableStart.substring(0, tableStart.indexOf(" "));
        }else {
            tableName = tableStart;
        }

        return tableName;
    }

    static String getIdNameFromQuery(String query) {
        String idNameStart = query.substring(query.indexOf("WHERE") + 6);
        return idNameStart.substring(0, idNameStart.indexOf(" "));
    }

    static int getIdValueFromQuery(String query){
        return Integer.parseInt(query.substring(query.indexOf("=") + 2));
    }

    private static List<String> getTableList(String path)  {
        File fileDir = new File(path);
        File[] files = fileDir.listFiles();
        List<String> tableList = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String tableName = fileName.substring(0, fileName.lastIndexOf("."));
                tableList.add(tableName);
            }
        }

        return tableList;
    }

    static String checkIfTableFromQueryExistsInDB(List<String> tableList, String tableName) throws ServerException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No such table '")
                .append(tableName)
                .append("' exists in the DataBase!");

        for (String table : tableList) {
            if(tableName.equals(table)){
                return table;
            }
        }
        throw new ServerException(stringBuilder.toString());
    }

    static List<String> getColumnNamesFromTable(String table, String path)  {
        List<String> columnNameList = new ArrayList<>();

        XSSFWorkbook workbook;
        try {
            FileInputStream fi = new FileInputStream(new File(path + table + DB_FILE_SUFFIX));
            workbook = new XSSFWorkbook(fi);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        Row firstRow = rowIterator.next();
        Iterator<Cell> cellIterator = firstRow.cellIterator();
        while (cellIterator.hasNext()){
            Cell cell = cellIterator.next();
            String stringCellValue = cell.getStringCellValue();
            columnNameList.add(stringCellValue);
        }
        return columnNameList;
    }

    static List<String> getColumnNamesFromInsertQuery(String query){
        return divideStringIntoList(query.substring(query.indexOf("(") + 1, query.indexOf(")")));
    }

    private static List<String> divideStringIntoList(String columnList){
        List<String> list = new ArrayList<>();
        String[] strings = columnList.split(", ");
        Collections.addAll(list, strings);
        return list;
    }

    static void areColumnsValid(List<String> columnListFromQuery, List<String> columnListFromTable, String table) throws ServerException {
        for (String columnFromQuery : columnListFromQuery) {
            if (!columnListFromTable.contains(columnFromQuery)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("No such column '")
                        .append(columnFromQuery)
                        .append("' in the table ")
                        .append(table);
                throw new ServerException(stringBuilder.toString());
            }
        }
    }

    static List<String> getColumnValuesFromInsertQuery(String query){
        return divideStringIntoList(query.substring(query.lastIndexOf("(") + 1, query.lastIndexOf(")")));
    }

    static Map<String, String> createMapFromQueryColNameToColValue(List<String> columnNames, List<String> columnValues){
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), columnValues.get(i));
        }

        return map;
    }

    private void insertDataIntoTable(Map<String, String> map, String table, String path){

        final String pId = "p_id";
        final String age = "age";
        final String id = "id";
        final String code = "code";
        final String number = "number";
        final String pName = "p_name";

        File file = new File(path + table + DB_FILE_SUFFIX);
        FileInputStream fi;
        try {
            fi = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fi);
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rowNum = sheet.getLastRowNum();

            Map<String, Integer> tableMap = new HashMap<>();
            Iterator<Row> rowIterator = sheet.iterator();
            Row firstRow = rowIterator.next();

            int columnIndex = 0;

            for(Map.Entry<String, String> entry: map.entrySet()){
                String key = entry.getKey();

                Iterator<Cell> cellIterator = firstRow.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String stringCellValue = cell.getStringCellValue();
                    if(key.equals(stringCellValue)){
                        columnIndex = cell.getColumnIndex();
                        break;
                    }

                }
                tableMap.put(key, columnIndex);
            }

            int newRowIndex = rowNum + 1;
            Row row = sheet.createRow(newRowIndex);

            for(Map.Entry<String, String> entry: map.entrySet()){
                String keyQuery = entry.getKey();

                for(Map.Entry<String, Integer> tableEntry: tableMap.entrySet()){
                    String keyTable = tableEntry.getKey();
                    if(keyQuery.equals(keyTable)){
                        Integer columnPosition = tableEntry.getValue();
                        Cell newCell = row.createCell(columnPosition);
                        if(keyTable.equalsIgnoreCase(pId) || keyTable.equalsIgnoreCase(age) || keyTable.equalsIgnoreCase(id)
                                || keyTable.equalsIgnoreCase(code) || keyTable.equalsIgnoreCase(number)){
                            newCell.setCellValue(new Double(entry.getValue()));
                        }else if(keyTable.equalsIgnoreCase(pName)){
                            newCell.setCellValue(entry.getValue());
                        }
                    }
                }
            }

            FileOutputStream fo = new FileOutputStream(file);
            workbook.write(fo);
            fo.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static int findCellNumByName(XSSFSheet sheet, String idNameFromQuery){
        Row firstRow = sheet.getRow(0);
        int cellNum = 0;
        short cellCountFirstRow = firstRow.getLastCellNum();
        for (int i = 0; i < cellCountFirstRow; i++) {
            Cell cell = firstRow.getCell(i);
            if(idNameFromQuery.equalsIgnoreCase(cell.getStringCellValue())){
                cellNum = i;
                break;
            }
        }
        return cellNum;
    }

    static int findRowIdByCellValue(XSSFSheet sheet, int rowCount, int cellNumByName, int id, String table){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No data in the table ")
                .append(table)
                .append(" with id value = ")
                .append(id);

        int rowNumForId = 0;
        for (int rowNum = 1; rowNum <= rowCount; rowNum++) {
            Row row = sheet.getRow(rowNum);
            Cell cell = row.getCell(cellNumByName);
            int numericCellValue = (int) cell.getNumericCellValue();

            if (id == numericCellValue) {
                rowNumForId = rowNum;
                break;
            }
        }
        if(rowNumForId == 0){
            throw new ServerException(stringBuilder.toString());
        }else {
            return rowNumForId;
        }
    }

    static Map<String, String> readDataFromTableById(String table,
                                                      String path, int id, String idNameFromQuery){
        File file = new File(path + table + DB_FILE_SUFFIX);
        FileInputStream fi;
        XSSFWorkbook workbook = null;
        try {
            fi = new FileInputStream(file);
            workbook = new XSSFWorkbook(fi);
        } catch (IOException e) {
            e.printStackTrace();
        }

        XSSFSheet sheet = workbook.getSheetAt(0);
        int rowCount = sheet.getLastRowNum();
        int cellNumByName = findCellNumByName(sheet, idNameFromQuery);
        int rowIdByCellValue = findRowIdByCellValue(sheet, rowCount, cellNumByName, id, table);

        //Get row for column names
        Row firstRow = sheet.getRow(0);
        //Get row for column values
        Row row = sheet.getRow(rowIdByCellValue);
        short cellCount = row.getLastCellNum();

        return getColumnNameToColumnValueMap(firstRow, row, cellCount);
    }

    static List<Map<String, String>> readAllDataFromTable(String table, String path){
        File file = new File(path + table + DB_FILE_SUFFIX);
        FileInputStream fi;
        XSSFWorkbook workbook = null;
        try {
            fi = new FileInputStream(file);
            workbook = new XSSFWorkbook(fi);
        } catch (IOException e) {
            e.printStackTrace();
        }
        XSSFSheet sheet = workbook.getSheetAt(0);
        int rowCount = sheet.getLastRowNum();
        List<Map<String, String>> dataMapList = new ArrayList<>();

        //Get row for column names
        Row firstRow = sheet.getRow(0);
        //Get row for column values
        for (int rowNum = 1; rowNum <= rowCount; rowNum++) {
            Row row = sheet.getRow(rowNum);

            short cellCount = row.getLastCellNum();
            dataMapList.add(getColumnNameToColumnValueMap(firstRow, row, cellCount));
        }
        return dataMapList;
    }

    static Map<String, String> getColumnNameToColumnValueMap(Row firstRow, Row row, short cellCount) {
        String stringCellValue;
        Map<String, String> columnNameToColumnValue = new HashMap<>();
        for (int cellNum = 0; cellNum < cellCount; cellNum++) {
            Cell columnName = firstRow.getCell(cellNum);
            Cell cell = row.getCell(cellNum);

            switch (cell.getCellType()){
                case Cell.CELL_TYPE_STRING:
                    stringCellValue = cell.getStringCellValue();
                    columnNameToColumnValue.put(columnName.getStringCellValue(), stringCellValue);
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    int numericCellValue = (int) cell.getNumericCellValue();
                    columnNameToColumnValue.put(columnName.getStringCellValue(), String.valueOf(numericCellValue));
                    break;
            }
        }
        return columnNameToColumnValue;
    }



    static void uniqueConstraintCheck(String tableName, Map<String, String> mapFromQueryColNameToColValue){
        String idColumnName = determineIdColumnName(tableName);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unique constraint (")
            .append(tableName.toUpperCase())
            .append(".")
            .append(idColumnName.toUpperCase())
            .append(") violated");

        List<String> idValuesListFromTable = getIdValuesListFromTable(tableName, idColumnName);
        if(idValuesListFromTable.contains(mapFromQueryColNameToColValue.get(idColumnName))){
            throw new ServerException(stringBuilder.toString());
        }
    }

    private static String determineIdColumnName(String tableName){
        String id = null;
        if(PERSONS.equals(tableName.toLowerCase())){
            id = "p_id";
        }else if(PHONES.equals(tableName.toLowerCase())){
            id = "id";
        }
        return id;
    }

    static List<String> getIdValuesListFromTable(String tableName, String idColumnName){
        File file = new File(PATH + tableName + DB_FILE_SUFFIX);
        FileInputStream fi;
        List<String> idList = new ArrayList<>();
        try {
            fi = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fi);
            XSSFSheet sheet = workbook.getSheetAt(0);
            int rowCount = sheet.getLastRowNum();
            int cellNumByName = findCellNumByName(sheet, idColumnName);

            for (int rowNum = 1; rowNum <= rowCount; rowNum++) {
                Row row = sheet.getRow(rowNum);
                Cell cell = row.getCell(cellNumByName);
                idList.add(String.valueOf((int)cell.getNumericCellValue()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return idList;
    }

    static List<String> getColumnNamesFromSelectQuery(String query) {
        String columnsFromSelectQuery = query.substring(7, query.indexOf("FROM") - 1);
        return divideStringIntoList(columnsFromSelectQuery);
    }

    static Map<String, String> getValuesById(String query){
        String tableName = getTableNameSelect(query);
        int idValueFromQuery = getIdValueFromQuery(query);
        String idNameFromQuery = getIdNameFromQuery(query);

        return readDataFromTableById(tableName, PATH, idValueFromQuery, idNameFromQuery);
    }

    static List<Map<String, String>> getAllValues(String query){
        String tableName = getTableNameSelect(query);
        List<Map<String, String>> allDataFromTable = readAllDataFromTable(tableName, PATH);

        if(allDataFromTable.size() == 0){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No data in the table ")
                    .append(tableName);
            throw new ServerException(stringBuilder.toString());
        }

        return allDataFromTable;
    }

}

