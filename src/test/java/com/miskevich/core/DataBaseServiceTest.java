package com.miskevich.core;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

import static org.testng.Assert.*;

public class DataBaseServiceTest {

    private final String QUERY_INSERT = "INSERT INTO persons(p_id, p_name, age) VALUES (1, name_1, 11)";
    private final String QUERY_BY_ID = "SELECT p_id, p_name, age FROM persons WHERE p_id = 1";
    private final String QUERY_ALL = "SELECT p_id, p_name, age FROM persons";
    private final List<String> TABLE_LIST = new ArrayList<String>(){{add("persons"); add("phones");}};
    private final String TABLE_NAME = "persons";
    private final String TABLE_NAME_INVALID = "invalidTableName";
    private final List<String> COLUMN_NAME_LIST = new ArrayList<String>(){{add("p_id"); add("p_name"); add("age");}};
    private final List<String> COLUMN_NAME_LIST_INVALID = new ArrayList<String>(){{add("p_id"); add("invalid_name"); add("age");}};
    private final List<String> COLUMN_VALUE_LIST = new ArrayList<String>(){{add("1"); add("name_1"); add("11");}};
    private final Map<String, String> COLUMN_NAME_TO_COLUMN_VALUE = new HashMap<String, String>()
    {{put("p_id", "1"); put("p_name", "name_1"); put("age", "11");}};
    private final Map<String, String> COLUMN_NAME_TO_COLUMN_VALUE_UNIQUE_CHECK_FALSE = new HashMap<String, String>()
    {{put("p_id", "100"); put("p_name", "name_100"); put("age", "100");}};
    private final String ID_NAME_FROM_QUERY = "p_id";
    private final int ID_VALUE_FROM_QUERY = 1;
    private final int CELL_NUM_BY_NAME = 0;
    private final int ROW_ID_BY_CELL_VALUE = 1;
    private XSSFSheet sheet;
    private int rowCount;
    private Row firstRow;
    private Row row;
    private short cellCount;
    private final List<Map<String, String>> ALL_DATA_FROM_TABLE = new ArrayList<Map<String, String>>(){{
        add(new LinkedHashMap<String, String>(){{put("p_id", "1"); put("p_name", "name_1"); put("age", "11");}});
        add(new LinkedHashMap<String, String>(){{put("p_id", "2"); put("p_name", "name_2"); put("age", "22");}});
    }};
    private final List<String> ID_LIST_FROM_TABLE = new ArrayList<String>(){{add("1"); add("2");}};
    private static final String DB_FILE_SUFFIX = ".xlsx";
    private static final String PATH = "src" + File.separator + "main" + File.separator + "java" + File.separator
            + "com" + File.separator + "miskevich" + File.separator + "data" + File.separator;


    @BeforeClass
    public void initializeFile(){
        cleanDataInFile(TABLE_NAME, PATH);
        populateFile();
        File file = new File(PATH + TABLE_NAME + DB_FILE_SUFFIX);
        FileInputStream fi;
        try {
            fi = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fi);
            sheet = workbook.getSheetAt(0);
            rowCount = sheet.getLastRowNum();
            firstRow = sheet.getRow(0);
            row = sheet.getRow(ROW_ID_BY_CELL_VALUE);
            cellCount = row.getLastCellNum();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetTableNameInsert(){
        String actual = DataBaseService.getTableNameInsert(QUERY_INSERT);
        assertEquals(actual, "persons");
    }

    @Test
    public void testGetTableNameSelect(){
        String actualById = DataBaseService.getTableNameSelect(QUERY_BY_ID);
        String actualAll = DataBaseService.getTableNameSelect(QUERY_ALL);
        assertEquals(actualById, "persons");
        assertEquals(actualAll, "persons");
    }

    @Test
    public void testGetIdNameFromQuery(){
        String actual = DataBaseService.getIdNameFromQuery(QUERY_BY_ID);
        assertEquals(actual, "p_id");
    }

    @Test
    public void testGetIdValueFromQuery(){
        int actual = DataBaseService.getIdValueFromQuery(QUERY_BY_ID);
        assertEquals(actual, 1);
    }

    @Test
    public void testCheckIfTableFromQueryExistsInDB(){
        String actual = DataBaseService.checkIfTableFromQueryExistsInDB(TABLE_LIST, TABLE_NAME);
        assertEquals(actual, TABLE_NAME);
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "No such table 'invalidTableName' exists in the DataBase!")
    public void testCheckIfTableFromQueryExistsInDBTableDoesNotExist(){
        DataBaseService.checkIfTableFromQueryExistsInDB(TABLE_LIST, TABLE_NAME_INVALID);
    }

    @Test
    public void testGetColumnNamesFromTable(){
        List<String> actual = DataBaseService.getColumnNamesFromTable(TABLE_NAME, PATH);
        assertEquals(actual, COLUMN_NAME_LIST);
    }

    @Test
    public void testGetColumnNamesFromInsertQuery(){
        List<String> actual = DataBaseService.getColumnNamesFromInsertQuery(QUERY_INSERT);
        assertEquals(actual, COLUMN_NAME_LIST);
    }

    @Test
    public void testAreColumnsValid(){
        DataBaseService.areColumnsValid(COLUMN_NAME_LIST, COLUMN_NAME_LIST, TABLE_NAME);
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "No such column 'invalid_name' in the table persons")
    public void testAreColumnsValidNoSuchColumnInDB(){
        DataBaseService.areColumnsValid(COLUMN_NAME_LIST_INVALID, COLUMN_NAME_LIST, TABLE_NAME);
    }

    @Test
    public void testGetColumnValuesFromInsertQuery(){
        List<String> actual = DataBaseService.getColumnValuesFromInsertQuery(QUERY_INSERT);
        assertEquals(actual, COLUMN_VALUE_LIST);
    }

    @Test
    public void testCreateMapFromQueryColNameToColValue(){
        Map<String, String> actual = DataBaseService.createMapFromQueryColNameToColValue(COLUMN_NAME_LIST, COLUMN_VALUE_LIST);
        assertEquals(actual, COLUMN_NAME_TO_COLUMN_VALUE);
    }

    @Test
    public void testFindCellNumByName(){
        int actual = DataBaseService.findCellNumByName(sheet, ID_NAME_FROM_QUERY);
        assertEquals(actual, CELL_NUM_BY_NAME);
    }

    @Test
    public void testFindRowIdByCellValue(){
        int actual = DataBaseService.findRowIdByCellValue(sheet, rowCount, CELL_NUM_BY_NAME, ID_VALUE_FROM_QUERY, TABLE_NAME);
        assertEquals(actual, ROW_ID_BY_CELL_VALUE);
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "No data in the table persons with id value = 111")
    public void testFindRowIdByCellValueNoData(){
        DataBaseService.findRowIdByCellValue(sheet, rowCount, CELL_NUM_BY_NAME, 111, TABLE_NAME);
    }

    @Test
    public void testReadDataFromTableById(){
        Map<String, String> actual = DataBaseService.readDataFromTableById(TABLE_NAME, PATH, ID_VALUE_FROM_QUERY, ID_NAME_FROM_QUERY);
        assertEquals(actual, COLUMN_NAME_TO_COLUMN_VALUE);
    }

    @Test
    public void testReadAllDataFromTable(){
        List<Map<String, String>> actual = DataBaseService.readAllDataFromTable(TABLE_NAME, PATH);
        assertEquals(actual, ALL_DATA_FROM_TABLE);
    }

    @Test
    public void testGetColumnNameToColumnValueMap(){
        Map<String, String> actual = DataBaseService.getColumnNameToColumnValueMap(firstRow, row, cellCount);
        assertEquals(actual, COLUMN_NAME_TO_COLUMN_VALUE);
    }

    @Test
    public void testUniqueConstraintCheck(){
        DataBaseService.uniqueConstraintCheck(TABLE_NAME, COLUMN_NAME_TO_COLUMN_VALUE_UNIQUE_CHECK_FALSE);
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "Unique constraint \\(PERSONS.P_ID\\) violated")
    public void testUniqueConstraintCheckTrue(){
        DataBaseService.uniqueConstraintCheck(TABLE_NAME, COLUMN_NAME_TO_COLUMN_VALUE);
    }

    @Test
    public void testGetIdValuesListFromTable(){
        List<String> actual = DataBaseService.getIdValuesListFromTable(TABLE_NAME, ID_NAME_FROM_QUERY);
        assertEquals(actual, ID_LIST_FROM_TABLE);
    }

    @Test
    public void testGetColumnNamesFromSelectQuery(){
        List<String> actual = DataBaseService.getColumnNamesFromSelectQuery(QUERY_BY_ID);
        assertEquals(actual, COLUMN_NAME_LIST);
    }

    @Test
    public void testGetValuesById(){
        Map<String, String> actual = DataBaseService.getValuesById(QUERY_BY_ID);
        assertEquals(actual, COLUMN_NAME_TO_COLUMN_VALUE);
    }

    @Test
    public void testGetAllValues(){
        List<Map<String, String>> actual = DataBaseService.getAllValues(QUERY_ALL);
        assertEquals(actual, ALL_DATA_FROM_TABLE);
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "No data in the table persons", enabled = false)
    public void testGetAllValuesEmptyTable(){
        cleanDataInFile(TABLE_NAME, PATH);
        DataBaseService.getAllValues(QUERY_ALL);
    }

    private void cleanDataInFile(String table, String path){
        File file = new File(path + table + DB_FILE_SUFFIX);
        FileInputStream fi;
        try {
            fi = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fi);
            XSSFSheet sheet = workbook.getSheetAt(0);

            for (int index = sheet.getLastRowNum(); index > sheet.getFirstRowNum(); index--) {
                sheet.removeRow( sheet.getRow(index));
            }

            FileOutputStream fo = new FileOutputStream(file);
            workbook.write(fo);
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void populateFile(){
        final String pId = "p_id";
        final String age = "age";
        final String pName = "p_name";

        File file = new File(PATH + TABLE_NAME + DB_FILE_SUFFIX);
        FileInputStream fi;
        try {
            fi = new FileInputStream(file);
            XSSFWorkbook workbook = new XSSFWorkbook(fi);
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i < ALL_DATA_FROM_TABLE.size(); i++) {
                Row row = sheet.createRow(i+1);
                for (int j = 0; j < ALL_DATA_FROM_TABLE.get(i).size(); j++) {
                    switch (j){
                        case 0: Cell cell = row.createCell(j);
                            cell.setCellValue(new Double(ALL_DATA_FROM_TABLE.get(i).get(pId)));
                            break;
                        case 1: cell = row.createCell(j);
                            cell.setCellValue(ALL_DATA_FROM_TABLE.get(i).get(pName));
                            break;
                        case 2: cell = row.createCell(j);
                            cell.setCellValue(new Double(ALL_DATA_FROM_TABLE.get(i).get(age)));
                            break;
                    }
                }
            }
            FileOutputStream fo = new FileOutputStream(file);
            workbook.write(fo);
            fo.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}