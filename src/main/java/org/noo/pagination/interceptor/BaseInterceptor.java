package org.noo.pagination.interceptor;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.xml.bind.PropertyException;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.plugin.Interceptor;
import org.noo.pagination.annotation.Paging;
import org.noo.pagination.dialect.Dialect;
import org.noo.pagination.dialect.db.MySQLDialect;
import org.noo.pagination.dialect.db.OracleDialect;
import org.noo.pagination.dialect.db.SQLServer2005Dialect;
import org.noo.pagination.page.Page;
import org.noo.pagination.page.Pagination;
import org.noo.pagination.uitls.Reflections;

/**
 * <p>
 * .
 * </p>
 * 
 * @author poplar.yfyang
 * @version 1.0 2012-05-12 上午10:21
 * @since JDK 1.5
 */
public abstract class BaseInterceptor implements Interceptor, Serializable {
	/**
	 * 日志
	 */
	protected Log log = LogFactory.getLog(this.getClass());

	protected static final String DELEGATE = "delegate";

	protected static final String MAPPED_STATEMENT = "mappedStatement";

	protected Dialect DIALECT;

	/**
	 * 拦截的ID，在mapper中的id，可以匹配正则
	 */
	protected String _SQL_PATTERN = "";
	private static final long serialVersionUID = 4596430444388728543L;

	/**
	 * 对参数进行转换和检查
	 * 
	 * @param parameterObject
	 *          参数对象
	 * @param pageVO
	 *          参数VO
	 * @return 参数VO
	 * @throws NoSuchFieldException
	 *           无法找到参数
	 */
	protected static Page convertParameter(Object parameterObject, Page pageVO) throws NoSuchFieldException {
		if (parameterObject instanceof Page) {
			pageVO = (Pagination) parameterObject;
		} else {
			// 参数为某个实体，该实体拥有Page属性
			Paging paging = parameterObject.getClass().getAnnotation(Paging.class);
			String field = paging.field();
			Field pageField = Reflections.getAccessibleField(parameterObject, field);
			if (pageField != null) {
				pageVO = (Pagination) Reflections.getFieldValue(parameterObject, field);
				if (pageVO == null)
					throw new PersistenceException("分页参数不能为空");
				// 通过反射，对实体对象设置分页对象
				Reflections.setFieldValue(parameterObject, field, pageVO);
			} else {
				throw new NoSuchFieldException(parameterObject.getClass().getName() + "不存在分页参数属性！");
			}
		}
		return pageVO;
	}

	/**
	 * 设置属性，支持自定义方言类和制定数据库的方式
	 * <p>
	 * <code>dialectClass</code>,自定义方言类。可以不配置这项 <ode>dbms</ode> 数据库类型，插件支持的数据库
	 * <code>sqlPattern</code> 需要拦截的SQL ID
	 * </p>
	 * 如果同时配置了<code>dialectClass</code>和<code>dbms</code>,则以<code>dbms</code>为主
	 * 
	 * @param p
	 *          属性
	 * @throws PropertyException
	 */
	protected void initProperties(Properties p) throws PropertyException {
		// String dbms = p.getProperty("dbms");
		// String dialectClass = "";//p.getProperty("dialectClass");
		// if(StringUtils.isBlank(dbms)){
		// throw new PropertyException("数据库分页方言无法找到!");
		// }
		// if(dbms.equalsIgnoreCase("mysql")){
		// dialectClass = MySQLDialect.class.getName();
		// }else if (dbms.equalsIgnoreCase("oracle")){
		// dialectClass = OracleDialect.class.getName();
		// }else if (dbms.equalsIgnoreCase("sqlserver2005")){
		// dialectClass = SQLServer2005Dialect.class.getName();
		// }else if (dbms.equalsIgnoreCase("sqlserver")){
		// dialectClass = SQLServerDialect.class.getName();
		// }
		//
		// if (StringUtils.isEmpty(dialectClass)) {
		// try {
		// throw new PropertyException("数据库分页方言无法找到!");
		// } catch (PropertyException e) {
		// e.printStackTrace();
		// }
		// } else {
		// Dialect dialect1 = (Dialect) Reflections.instance(dialectClass);
		// if (dialect1 == null) {
		// throw new NullPointerException("方言实例错误");
		// }
		// DIALECT = dialect1;
		// }

		_SQL_PATTERN = p.getProperty("sqlPattern");
		if (StringUtils.isEmpty(_SQL_PATTERN)) {
			try {
				throw new PropertyException("sqlPattern property is not found!");
			} catch (PropertyException e) {
				e.printStackTrace();
			}
		}
	}

	protected Dialect getDialect(Connection conn) {
		String productName = "";
		try {
			productName = conn.getMetaData().getDatabaseProductName();
			//System.out.println(productName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String dbms = databaseTypeMappings.getProperty(productName);
		String dialectClass = "";
		if (dbms.equalsIgnoreCase("mysql")) {
			dialectClass = MySQLDialect.class.getName();
		} else if (dbms.equalsIgnoreCase("oracle")) {
			dialectClass = OracleDialect.class.getName();
		} else if (dbms.equalsIgnoreCase("mssql")) {
			dialectClass = SQLServer2005Dialect.class.getName();
		}

		if (StringUtils.isEmpty(dialectClass)) {
			try {
				throw new PropertyException("数据库分页方言无法找到!");
			} catch (PropertyException e) {
				e.printStackTrace();
			}
		} else {
			Dialect dialect1 = (Dialect) Reflections.instance(dialectClass);
			if (dialect1 == null) {
				throw new NullPointerException("方言实例错误");
			}
			return dialect1;
		}
		return null;
	}

	protected static Properties databaseTypeMappings = getDefaultDatabaseTypeMappings();

	protected static Properties getDefaultDatabaseTypeMappings() {
		Properties databaseTypeMappings = new Properties();
		databaseTypeMappings.setProperty("H2", "h2");
		databaseTypeMappings.setProperty("MySQL", "mysql");
		databaseTypeMappings.setProperty("Oracle", "oracle");
		databaseTypeMappings.setProperty("PostgreSQL", "postgres");
		databaseTypeMappings.setProperty("Microsoft SQL Server", "mssql");
		databaseTypeMappings.setProperty("DB2", "db2");
		databaseTypeMappings.setProperty("DB2", "db2");
		databaseTypeMappings.setProperty("DB2/NT", "db2");
		databaseTypeMappings.setProperty("DB2/NT64", "db2");
		databaseTypeMappings.setProperty("DB2 UDP", "db2");
		databaseTypeMappings.setProperty("DB2/LINUX", "db2");
		databaseTypeMappings.setProperty("DB2/LINUX390", "db2");
		databaseTypeMappings.setProperty("DB2/LINUXX8664", "db2");
		databaseTypeMappings.setProperty("DB2/LINUXZ64", "db2");
		databaseTypeMappings.setProperty("DB2/400 SQL", "db2");
		databaseTypeMappings.setProperty("DB2/6000", "db2");
		databaseTypeMappings.setProperty("DB2 UDB iSeries", "db2");
		databaseTypeMappings.setProperty("DB2/AIX64", "db2");
		databaseTypeMappings.setProperty("DB2/HPUX", "db2");
		databaseTypeMappings.setProperty("DB2/HP64", "db2");
		databaseTypeMappings.setProperty("DB2/SUN", "db2");
		databaseTypeMappings.setProperty("DB2/SUN64", "db2");
		databaseTypeMappings.setProperty("DB2/PTX", "db2");
		databaseTypeMappings.setProperty("DB2/2", "db2");
		return databaseTypeMappings;
	}

}
