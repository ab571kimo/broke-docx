package utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 虛擬VO (測試)
 * 
 * @author 楊政昌
 *
 */
@Slf4j
@SuppressWarnings({ "rawtypes", "unchecked" })
public class VirtualVo {

	private List<String> TableColumn = null;

	private List<String> TableKey = null;

	private List<String> NotNullColumn = new ArrayList();

	private Map RemarksMap = new HashMap();

	private Map LengthMap = new HashMap();

	private String SCHEMA = null;

	private String TABLENAME = null;

	private String theSql = null;

	private List<String> columnList = new ArrayList();

	/**
	 * for batch
	 */
	private List<Condition> conditionList = new ArrayList();

	private List<String> orderList = new ArrayList();

	private List<String> statementList = new ArrayList();

	private Map updateMap = new HashMap();

	private Map<String, Object> insertMap = new HashMap();

	private Connection connection;

	private static final String JDBC_URL = "jdbc:db2://localhost:50000/testdb";
	private static final String USERNAME = "db2inst1";
	private static final String PASSWORD = "INSTPW";

	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
	}
	/**
	 * 準備寫入TABLE
	 * 
	 * @param theSCHEMA
	 * @param theTABLENAME
	 * @throws Exception @
	 */
	public VirtualVo(Connection conn,String theSCHEMA, String theTABLENAME) throws Exception {

		connection = conn;

		PreparedStatement pstmt = connection
				.prepareStatement("SELECT COLNAME,KEYSEQ,TYPENAME,NULLS,LENGTH,REMARKS FROM SYSCAT.COLUMNS WHERE TABSCHEMA = ? AND TABNAME = ?");

		pstmt.setString(1, theSCHEMA);
		pstmt.setString(2, theTABLENAME);

		ResultSet rs = pstmt.executeQuery();
		List<Map> rows = resultSetToArrayList(rs);

		TableColumn = new ArrayList();
		TableKey = new ArrayList();

		for (Map map : rows) {
			String NAME = MapUtils.getString(map, "COLNAME");
			String KEYSEQ = MapUtils.getString(map, "KEYSEQ");
			String COLTYPE = MapUtils.getString(map, "TYPENAME");
			String NULLS = MapUtils.getString(map, "NULLS");
			String LENGTH = MapUtils.getString(map, "LENGTH");
			String REMARKS = MapUtils.getString(map, "REMARKS");

			TableColumn.add(NAME);
			RemarksMap.put(NAME, REMARKS);

			//key
			if (StringUtils.isNotBlank(KEYSEQ)) {
				TableKey.add(NAME);
			}
			//非空欄位
			if ("Y".equals(NULLS)) {
				NotNullColumn.add(NAME);
			}
			//字元長度
			if ("VARCHAR ".equals(COLTYPE)) {
				LengthMap.put(NAME, LENGTH);
			}
		}

		this.SCHEMA = theSCHEMA;
		this.TABLENAME = theTABLENAME;
	}

	/**
	 * 放入顯示欄位
	 * 
	 * @param key
	 * @throws Exception
	 */
	public void setColumn(String key) {
		columnList.add(key);
	}

	/**
	 * 放入排序
	 * 
	 * @param key
	 * @param sort [ 大於0 : 順向 ASC , 小於0 : 逆向 DESC , 等於0 : 略過] @
	 */
	public void setOrder(String key, int sort) {
		if (sort > 0) {
			orderList.add(key + ' ' + "ASC");
		} else if (sort < 0) {
			orderList.add(key + ' ' + "DESC");
		}
	}

	/**
	 * 放入顯示欄位
	 * 
	 * @param keys
	 * @throws Exception
	 */
	public void setColumn(String[] keys) {
		columnList.addAll(Arrays.asList(keys));
	}

	/**
	 * 放入查詢條件
	 * 
	 * @param key
	 * @param value (can be null)
	 * @throws Exception
	 */
	public void setCondition(String key, Object value) {
		if (TableColumn.contains(key)) {
			Condition condition = new Condition();
			condition.setKey(key);
			condition.setSymbol(VoSymbol.EQUAL);
			condition.setValue(value);
			conditionList.add(condition);
		} else {
			log.debug("TableColumn 不含 " + key);
		}
	}

	/**
	 * 放入特殊查詢條件
	 * 
	 * @param key
	 * @param symbol (大於、小於、IN、LIKE...)
	 * @param value
	 * @throws Exception
	 */
	public void setCondition(String key, VoSymbol symbol, Object value) throws Exception {

		if (value == null && symbol != VoSymbol.NOT) {
			throw new Exception("值不得為空");
		}

		if (TableColumn.contains(key)) {
			Condition condition = new Condition();
			condition.setKey(key);
			condition.setSymbol(symbol);
			condition.setValue(value);
			conditionList.add(condition);
		} else {
			log.debug("TableColumn 不含 " + key);
		}
	}

	/**
	 * 使用Map中所有條件
	 * 
	 * @param map
	 * @throws Exception
	 */
	public void setCondition(Map map) {
		for (Object object : map.keySet()) {
			String key = (String) object;
			if (TableColumn.contains(key)) {
				Condition condition = new Condition();
				condition.setKey(key);
				condition.setSymbol(VoSymbol.EQUAL);
				condition.setValue(map.get(key));
				conditionList.add(condition);
			} else {
				log.debug("TableColumn 不含 " + key);
			}
		}
	}

	/**
	 * 使用PK為條件 (必須包含所有的PK)
	 * 
	 * @param map
	 * @throws Exception
	 */
	public void setConditionByPk(Map map) throws Exception {
		for (String pk : TableKey) {
			if (map.containsKey(pk)) {
				Condition condition = new Condition();
				condition.setKey(pk);
				condition.setSymbol(VoSymbol.EQUAL);
				condition.setValue(map.get(pk));
				conditionList.add(condition);
			} else {
				throw new Exception("更新條件不含 Primary Key : " + pk);
			}
		}
	}

	/**
	 * 放入更新值
	 * 
	 * @param key
	 * @param value (can be null)
	 * @throws Exception
	 */
	public void setUpdate(String key, Object value) {
		updateMap.put(key, value);
	}

	/**
	 * 放入新增值
	 * 
	 * @param key
	 * @param value (can be null)
	 * @throws Exception
	 */
	public void setInsert(String key, Object value) {
		insertMap.put(key, value);
	}

	/**
	 * 放入新增值 (多筆)
	 * 
	 * @param map
	 * @throws Exception
	 */
	public void setInsert(Map map) {
		for (String column : TableColumn) {
			if (map.containsKey(column)) {
				setInsert(column, map.get(column));
			}
		}
	}

	/**
	 * 查詢
	 * 
	 * @param dataNotFoundIsError
	 * @return
	 * @throws Exception
	 */
	public List<Map> select(boolean dataNotFoundIsError) throws Exception {

		List<Map> list = doSelect(dataNotFoundIsError);

		this.clear();

		return list == null ? new ArrayList() : list;
	}

	/**
	 * 查詢
	 * 
	 * @param dataNotFoundIsError
	 * @return
	 * @throws Exception
	 */
	private List<Map> doSelect(boolean dataNotFoundIsError) throws Exception {

		PreparedStatement pstmt = connection.prepareStatement(getSelectSQL());

		int i = 1;
		for (String string : statementList) {
			pstmt.setString(i, string);
			i++;
		}

		log.debug("【本次SQL語法】" + getSqlLog());

		ResultSet rs = pstmt.executeQuery();
		List<Map> rows = resultSetToArrayList(rs);

		log.debug("【本次查詢件數】" + rows.size());

		return rows;
	}

	/**
	 * 取得查詢SQL
	 * 
	 * @return
	 * @throws Exception @
	 */
	protected String getSelectSQL() throws Exception {

		statementList.clear();

		StringBuilder sb = new StringBuilder();

		sb.append("SELECT ");

		boolean first = true;
		if (columnList.isEmpty()) {
			sb.append('*');
		} else {
			for (String column : columnList) {
				if (!first) {
					sb.append(" , ");
				}
				sb.append(column);
				first = false;
			}
		}

		sb.append(" FROM ").append(SCHEMA).append('.').append(TABLENAME);

		if (conditionList == null || conditionList.isEmpty()) {
			throw new Exception("條件參數不得為空");
		}

		// 組出SQL條件
		appendWhere(sb);

		// 排序
		if (!orderList.isEmpty()) {
			first = true;
			sb.append(" ORDER BY ");
			for (String order : orderList) {
				if (!first) {
					sb.append(",");
				}
				sb.append(order);
			}
		}

		return sb.toString();
	}

	/**
	 * 組出可執行SQL語法 (僅查詢)
	 * 
	 * @return @
	 * @throws Exception
	 */
	public String getSqlLog() throws Exception {

		if (conditionList.isEmpty()) {
			throw new Exception("尚未設定查詢參數");
		}

		StringBuilder sqlLog = new StringBuilder(getSelectSQL());

		for (String string : statementList) {
			int index = sqlLog.indexOf("?");
			sqlLog.replace(index, index + 1, string);
		}

		return sqlLog.toString();
	}

	/**
	 * 組出SQL條件參數
	 * 
	 * @return @
	 * @throws Exception
	 */
	private void appendWhere(StringBuilder sb) throws Exception {

		sb.append(" WHERE ");

		boolean first = true;
		for (Condition condition : conditionList) {

			String keyColumn = condition.getKey();
			Object value = condition.getValue();
			VoSymbol symbol = condition.getSymbol();

			if (!first) {
				sb.append(" AND ");
			}

			if (value == null) {
				// 條件為空值
				if (symbol != VoSymbol.NOT) {
					sb.append(keyColumn).append(" IS NULL ");
				} else {
					sb.append(keyColumn).append(" IS NOT NULL ");
				}
			} else {

				if (symbol != VoSymbol.EQUAL) {

					switch (symbol) {

					case NOT:
						// 大於
						sb.append(keyColumn).append(" != ?");
						statementList.add(value.toString());
						break;
					case MORE:
						// 大於
						sb.append(keyColumn).append(" > ?");
						statementList.add(value.toString());
						break;
					case MORE_OR_EQUAL:
						// 大於等於
						sb.append(keyColumn).append(" >= ?");
						statementList.add(value.toString());
						break;
					case LESS:
						// 小於
						sb.append(keyColumn).append(" < ?");
						statementList.add(value.toString());
						break;
					case LESS_OR_EQUAL:
						// 小於等於
						sb.append(keyColumn).append(" <= ?");
						statementList.add(value.toString());
						break;
					case IN:
						// IN
						sb.append(keyColumn).append(" IN (");
						if (value instanceof List) {
							List<String> valueList = (List) value;
							for (String string : valueList) {
								sb.append(" ? ,");
								statementList.add(string);
							}
						} else if (value instanceof String[]) {
							String[] valueList = (String[]) value;
							for (String string : valueList) {
								sb.append(" ? ,");
								statementList.add(string);
							}
						}
						sb.delete(sb.length() - 1, sb.length()).append(")");
						break;
					case LIKE:
						// LIKE
						sb.append(keyColumn).append(" LIKE ?");
						statementList.add(value.toString());
						break;
					case NOT_IN:
						// NOT IN
						sb.append(keyColumn).append(" NOT IN (");
						if (value instanceof List) {
							List<String> valueList = (List) value;
							for (String string : valueList) {
								sb.append(" ? ,");
								statementList.add(string);
							}
						} else if (value instanceof String[]) {
							String[] valueList = (String[]) value;
							for (String string : valueList) {
								sb.append(" ? ,");
								statementList.add(string);
							}
						}
						sb.delete(sb.length() - 1, sb.length()).append(")");
						break;
					case NOT_LIKE:
						// NOT LIKE
						sb.append(keyColumn).append(" NOT LIKE ?");
						statementList.add(value.toString());
						break;
					default:
						throw new Exception("無此特殊條件");
					}

				} else {
					// 條件非空值 & 特殊
					sb.append(keyColumn).append(" = ?");
					statementList.add(value.toString());
				}

			}

			first = false;
		}

	}

	/**
	 * 組出SQL條件參數 (只有KEY值)
	 * 
	 * @return @
	 * @throws Exception
	 */
	private void appendWhereByKey(StringBuilder sb) throws Exception {

		if (TableKey.isEmpty()) {
			throw new Exception("TableKey undefine");
		}

		sb.append(" WHERE ");

		boolean first = true;
		for (String key : TableKey) {

			if (!first) {
				sb.append(" AND ");
			}

			sb.append(key).append(" = ?");

			first = false;
		}
	}

	public void insertByBatch(List<Map> dataMapList, boolean dupeIsError) throws Exception {

		long start = System.currentTimeMillis();

		if (dataMapList == null) {
			throw new Exception("傳入物件不可為空");
		}

		if (dataMapList.isEmpty()) {
			log.debug("傳入物件無資料，略過寫入");
			return;
		}

		// 整理未設定的欄位，放入空值
		for (Map map : dataMapList) {
			for (String column : TableColumn) {
				map.put(column, map.get(column));
			}
		}

		PreparedStatement pstmt = connection.prepareStatement(getInsertSQL());

		for (Map<String, Object> map : dataMapList) {
			int i = 1;
			for (String keyColum : TableColumn) {
				pstmt.setString(i, MapUtils.getString(map, keyColum));
				i++;
			}
			pstmt.addBatch();
		}
		int[] resultCnt = pstmt.executeBatch();

		long end = System.currentTimeMillis();
		int successCount = 0;
		for (int result : resultCnt) {
			if (result >= 0) {
				successCount = successCount + result;
			}
		}

		log.debug(String.format("【本次新增筆數 %d 筆】", successCount));
		log.debug(String.format("【花費時間 : %d 毫秒】", (end - start)));

		this.clear();
	}

	public String getInsertSQL() {

		StringBuilder sb = new StringBuilder();

		sb.append("INSERT INTO ").append(SCHEMA).append('.').append(TABLENAME).append(" ( ");

		boolean first = true;
		for (String keyColum : TableColumn) {

			if (!first) {
				sb.append(" , ");
			}
			sb.append(keyColum);

			first = false;
		}

		sb.append(" ) VALUES ( ");

		first = true;
		for (int i = 0; i < TableColumn.size(); i++) {
			if (!first) {
				sb.append(" , ");
			}
			sb.append(" ?");

			first = false;
		}

		sb.append(" ) ");

		theSql = sb.toString();

		return theSql;

	}

	public void insertNotExistByBatch(List<Map> dataMapList, boolean dupeIsError) throws Exception {

		long start = System.currentTimeMillis();

		if (dataMapList == null) {
			throw new Exception("傳入物件不可為空");
		}

		if (dataMapList.isEmpty()) {
			log.debug("傳入物件無資料，略過寫入");
			return;
		}

		// 整理未設定的欄位，放入空值
		for (Map map : dataMapList) {
			for (String column : TableColumn) {
				map.put(column, map.get(column));
			}
		}

		int cnt = getRecordnum();

		if (cnt <= 0) {
			log.debug(String.format("資料表無資料，本次使用insertByBatch"));
			insertByBatch(dataMapList, dupeIsError);
			return;
		}

		PreparedStatement pstmt = connection.prepareStatement(getInsertNotExistSQL());

		for (Map<String, Object> map : dataMapList) {
			int i = 1;
			for (String keyColum : TableColumn) {
				pstmt.setString(i, MapUtils.getString(map, keyColum));
				i++;
			}

			for (String keyColum : TableKey) {
				pstmt.setString(i, MapUtils.getString(map, keyColum));
				i++;
			}
			pstmt.addBatch();
		}
		int[] resultCnt = pstmt.executeBatch();

		long end = System.currentTimeMillis();
		int successCount = 0;
		for (int result : resultCnt) {
			if (result >= 0) {
				successCount = successCount + result;
			}
		}

		log.debug(String.format("【本次新增筆數 %d 筆】", successCount));
		log.debug(String.format("【花費時間 : %d 毫秒】", (end - start)));

		this.clear();
	}

	public String getInsertNotExistSQL() throws Exception {

		StringBuilder sb = new StringBuilder();

		sb.append("INSERT INTO ").append(SCHEMA).append('.').append(TABLENAME).append(" ( ");

		boolean first = true;
		for (String keyColum : TableColumn) {

			if (!first) {
				sb.append(" , ");
			}
			sb.append(keyColum);

			first = false;
		}

		sb.append(" ) SELECT ");

		first = true;
		for (int i = 0; i < TableColumn.size(); i++) {
			if (!first) {
				sb.append(" , ");
			}
			sb.append(" ?");

			first = false;
		}

		sb.append(" FROM ").append(SCHEMA).append('.').append(TABLENAME);
		sb.append(" WHERE NOT EXISTS ( ");

		sb.append(" SELECT * FROM ").append(SCHEMA).append('.').append(TABLENAME);
		appendWhereByKey(sb);
		sb.append(" ) LIMIT 1 ");

		theSql = sb.toString();

		return theSql;

	}

	/**
	 * 組出更新SQL
	 * 
	 * @return @
	 * @throws Exception
	 */
	public String getUpdateSQL() throws Exception {

		statementList.clear();

		StringBuilder sb = new StringBuilder();

		sb.append("UPDATE ").append(SCHEMA).append('.').append(TABLENAME).append(" SET ");

		boolean first = true;
		for (Object object : updateMap.keySet()) {

			String updateColum = (String) object;

			if (!first) {
				sb.append(" , ");
			}
			sb.append(updateColum).append(" = ?");
			statementList.add(MapUtils.getString(updateMap, updateColum));
			first = false;
		}

		if (conditionList == null || conditionList.isEmpty()) {
			throw new Exception("條件參數不得為空");
		}

		// 組出SQL條件
		appendWhereByKey(sb);

		theSql = sb.toString();

		return theSql;

	}

	/**
	 * 組出刪除SQL
	 * 
	 * @return
	 * @throws Exception @
	 */
	protected String getDeleteSQL() throws Exception {

		StringBuilder sb = new StringBuilder();

		sb.append("DELETE FROM ").append(SCHEMA).append('.').append(TABLENAME).append(" ");

		if (conditionList == null || conditionList.isEmpty()) {
			throw new Exception("條件參數不得為空");
		}

		// 組出SQL條件
		appendWhereByKey(sb);

		theSql = sb.toString();

		return theSql;

	}

	/**
	 * 新增
	 * 
	 * @param dupeIsError
	 * @throws Exception
	 */
	public void insert(boolean dupeIsError) throws Exception {

		if (insertMap == null || insertMap.isEmpty()) {
			throw new Exception("傳入物件不可為空");
		}

		List<Map> list = new ArrayList();
		list.add(insertMap);

		insertByBatch(list, dupeIsError);

		this.clear();
	}

	/**
	 * 修改
	 * 
	 * @param dataNotFoundIsError
	 * @return 修改前資料
	 * @throws Exception
	 */
	public List<Map> updateByBatch(boolean dataNotFoundIsError) throws Exception {

		long start = System.currentTimeMillis();

		if (conditionList == null || conditionList.isEmpty()) {
			throw new Exception("查詢欄位不可為空"); // 查詢欄位名稱不可為空
		}
		if (updateMap == null || updateMap.isEmpty()) {
			throw new Exception("更新欄位不可為空"); // 查詢欄位名稱不可為空
		}

		List<Map> tmpList = doSelect(dataNotFoundIsError);

		PreparedStatement pstmt = connection.prepareStatement(getUpdateSQL());

		for (Map<String, Object> map : tmpList) {
			int i = 1;

			// update參數
			for (String statement : statementList) {
				pstmt.setString(i, statement);
				i++;
			}

			// key參數
			for (String key : TableKey) {
				pstmt.setString(i, MapUtils.getString(map, key));
				i++;
			}
			// pstmt.execute();
			pstmt.addBatch();
		}
		pstmt.executeBatch();

		long end = System.currentTimeMillis();

		log.debug(String.format("【本次更新筆數 %d 筆】", tmpList.size()));
		log.debug(String.format("【花費時間 : %d 毫秒】", (end - start)));

		this.clear();

		return tmpList;
	}

	/**
	 * 修改
	 * 
	 * @param dataNotFoundIsError
	 * @return 修改前資料
	 * @throws Exception
	 */
	public List<Map> deleteByBatch(boolean dataNotFoundIsError) throws Exception {

		long start = System.currentTimeMillis();

		if (conditionList == null || conditionList.isEmpty()) {
			throw new Exception("查詢欄位不可為空"); // 查詢欄位名稱不可為空
		}

		List<Map> tmpList = doSelect(dataNotFoundIsError);

		PreparedStatement pstmt = connection.prepareStatement(getDeleteSQL());

		// 更新
		for (Map<String, Object> map : tmpList) {
			int i = 1;

			// key參數
			for (String key : TableKey) {
				pstmt.setString(i, MapUtils.getString(map, key));
				i++;
			}
			// pstmt.execute();
			pstmt.addBatch();
		}
		pstmt.executeBatch();

		long end = System.currentTimeMillis();

		log.debug(String.format("【本次刪除筆數 %d 筆】", tmpList.size()));
		log.debug(String.format("【花費時間 : %d 毫秒】", (end - start)));

		this.clear();

		return tmpList;
	}

	/**
	 * 清除條件參數
	 * 
	 * @return
	 */
	public void clear() {
		insertMap.clear();
		updateMap.clear();
		orderList.clear();
		conditionList.clear();
		statementList.clear();
	}

	public enum VoSymbol {

		/**
		 * =
		 */
		EQUAL,

		/**
		 * >
		 */
		MORE,

		/**
		 * >=
		 */
		MORE_OR_EQUAL,

		/**
		 * <
		 */
		LESS,

		/**
		 * <=
		 */
		LESS_OR_EQUAL,

		IN,

		LIKE,

		NOT,

		NOT_IN,

		NOT_LIKE

	}

	public List<Map> resultSetToArrayList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		ArrayList list = new ArrayList();
		while (rs.next()) {
			Map row = new HashMap();
			for (int i = 1; i <= columns; ++i) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
			list.add(row);
		}
		return list;
	}

	private int getRecordnum() throws SQLException {

		PreparedStatement pstmt = connection
				.prepareStatement("SELECT COUNT(*) AS TABLE_ROWS FROM " + SCHEMA + "." + TABLENAME);

		// pstmt.setString(1, SCHEMA);
		// pstmt.setString(2, TABLENAME);

		ResultSet rs = pstmt.executeQuery();
		List<Map> rows = resultSetToArrayList(rs);

		return MapUtils.getInteger(rows.get(0), "TABLE_ROWS");

	}

	@Data
	private class Condition {
		private String key;
		private Object value;
		private VoSymbol symbol;
	}
}