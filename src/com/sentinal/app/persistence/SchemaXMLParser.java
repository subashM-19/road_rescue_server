package com.sentinal.app.persistence;

import com.sentinal.persistencefw.metadata.ColumnMetaData;
import com.sentinal.persistencefw.metadata.Schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class SchemaXMLParser
{
	public void load(File schemaFile)
	{
		if (schemaFile == null || !schemaFile.exists())
		{
			throw new IllegalArgumentException(
				"Schema file does not exist: " + schemaFile
			);
		}

		try
		{
			DocumentBuilderFactory factory =
				DocumentBuilderFactory.newInstance();

			factory.setFeature(
				"http://apache.org/xml/features/disallow-doctype-decl",
				true
			);

			factory.setFeature(
				"http://xml.org/sax/features/external-general-entities",
				false
			);

			factory.setFeature(
				"http://xml.org/sax/features/external-parameter-entities",
				false
			);

			factory.setAttribute(
				XMLConstants.ACCESS_EXTERNAL_DTD,
				""
			);

			factory.setAttribute(
				XMLConstants.ACCESS_EXTERNAL_SCHEMA,
				""
			);

			Document document = factory
				.newDocumentBuilder()
				.parse(schemaFile);

			document.getDocumentElement().normalize();

			if (!"Schema".equals(
				document.getDocumentElement().getTagName()))
			{
				throw new IllegalArgumentException(
					"Root element must be Schema"
				);
			}

			Schema.clear();

			NodeList tableNodes =
				document.getDocumentElement().getChildNodes();

			for (int i = 0; i < tableNodes.getLength(); i++)
			{
				Node node = tableNodes.item(i);

				if (node.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}

				Element tableElement =
					(Element) node;

				if (!"Table".equals(
					tableElement.getTagName()))
				{
					continue;
				}

				registerTable(
					tableElement
				);
			}

			Schema.validate();
		}
		catch (Exception e)
		{
			Schema.clear();

			throw new IllegalStateException(
				"Failed to load persistence schema from "
					+ schemaFile.getAbsolutePath(),
				e
			);
		}
	}

	private void registerTable(
		Element tableElement)
	{
		String tableName =
			getRequiredAttribute(
				tableElement,
				"name"
			);

		List<ColumnMetaData> columns =
			new ArrayList<>();

		NodeList nodes =
			tableElement.getChildNodes();

		for (int i = 0; i < nodes.getLength(); i++)
		{
			Node node = nodes.item(i);

			if (node.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}

			Element element =
				(Element) node;

			if (!"Column".equals(
				element.getTagName()))
			{
				continue;
			}

			columns.add(
				createColumn(element)
			);
		}

		if (columns.isEmpty())
		{
			throw new IllegalArgumentException(
				"Table " + tableName
					+ " does not contain any columns"
			);
		}

		Schema.registerTable(
			tableName,
			columns.toArray(
				new ColumnMetaData[0]
			)
		);
	}

	private ColumnMetaData createColumn(
		Element element)
	{
		String name =
			getRequiredAttribute(
				element,
				"name"
			);

		String jdbcTypeName =
			getRequiredAttribute(
				element,
				"jdbcType"
			);

		String typeName =
			getAttribute(
				element,
				"typeName",
				jdbcTypeName
			);

		int size =
			getIntegerAttribute(
				element,
				"size",
				0
			);

		boolean nullable =
			getBooleanAttribute(
				element,
				"nullable",
				true
			);

		boolean autoIncrement =
			getBooleanAttribute(
				element,
				"autoIncrement",
				false
			);

		boolean primaryKey =
			getBooleanAttribute(
				element,
				"primaryKey",
				false
			);

		boolean unique =
			getBooleanAttribute(
				element,
				"unique",
				false
			);

		boolean lastModifiedTime =
			getBooleanAttribute(
				element,
				"lastModifiedTime",
				false
			);

		String referencedTable =
			getNullableAttribute(
				element,
				"referencedTable"
			);

		String referencedColumn =
			getNullableAttribute(
				element,
				"referencedColumn"
			);

		Object defaultValue =
			parseDefaultValue(
				getNullableAttribute(
					element,
					"defaultValue"
				),
				jdbcTypeName
			);

		return new ColumnMetaData(
			name,
			resolveJdbcType(
				jdbcTypeName
			),
			typeName,
			size,
			nullable,
			autoIncrement,
			primaryKey,
			unique,
			defaultValue,
			lastModifiedTime,
			referencedTable,
			referencedColumn
		);
	}

	private int resolveJdbcType(
		String jdbcTypeName)
	{
		switch (jdbcTypeName.toUpperCase())
		{
			case "BIGINT":
				return Types.BIGINT;

			case "INTEGER":
			case "INT":
				return Types.INTEGER;

			case "SMALLINT":
				return Types.SMALLINT;

			case "TINYINT":
				return Types.TINYINT;

			case "VARCHAR":
				return Types.VARCHAR;

			case "CHAR":
				return Types.CHAR;

			case "BOOLEAN":
				return Types.BOOLEAN;

			case "DOUBLE":
				return Types.DOUBLE;

			case "FLOAT":
				return Types.FLOAT;

			case "DECIMAL":
				return Types.DECIMAL;

			case "NUMERIC":
				return Types.NUMERIC;

			case "DATE":
				return Types.DATE;

			case "TIME":
				return Types.TIME;

			case "TIMESTAMP":
				return Types.TIMESTAMP;

			default:
				throw new IllegalArgumentException(
					"Unsupported JDBC type: "
						+ jdbcTypeName
				);
		}
	}

	private Object parseDefaultValue(
		String value,
		String jdbcTypeName)
	{
		if (value == null)
		{
			return null;
		}

		switch (jdbcTypeName.toUpperCase())
		{
			case "BIGINT":
				return Long.valueOf(value);

			case "INTEGER":
			case "INT":
			case "SMALLINT":
			case "TINYINT":
				return Integer.valueOf(value);

			case "DOUBLE":
			case "FLOAT":
			case "DECIMAL":
			case "NUMERIC":
				return Double.valueOf(value);

			case "BOOLEAN":
				return Boolean.valueOf(value);

			default:
				return value;
		}
	}

	private String getRequiredAttribute(
		Element element,
		String name)
	{
		String value =
			element.getAttribute(name);

		if (value == null ||
			value.trim().isEmpty())
		{
			throw new IllegalArgumentException(
				"Missing required attribute '"
					+ name
					+ "' in "
					+ element.getTagName()
			);
		}

		return value.trim();
	}

	private String getAttribute(
		Element element,
		String name,
		String defaultValue)
	{
		String value =
			element.getAttribute(name);

		if (value == null ||
			value.trim().isEmpty())
		{
			return defaultValue;
		}

		return value.trim();
	}

	private String getNullableAttribute(
		Element element,
		String name)
	{
		String value =
			element.getAttribute(name);

		if (value == null ||
			value.trim().isEmpty())
		{
			return null;
		}

		return value.trim();
	}

	private boolean getBooleanAttribute(
		Element element,
		String name,
		boolean defaultValue)
	{
		String value =
			getNullableAttribute(
				element,
				name
			);

		if (value == null)
		{
			return defaultValue;
		}

		if (!"true".equalsIgnoreCase(value) &&
			!"false".equalsIgnoreCase(value))
		{
			throw new IllegalArgumentException(
				"Invalid boolean value for "
					+ name
					+ ": "
					+ value
			);
		}

		return Boolean.parseBoolean(
			value
		);
	}

	private int getIntegerAttribute(
		Element element,
		String name,
		int defaultValue)
	{
		String value =
			getNullableAttribute(
				element,
				name
			);

		if (value == null)
		{
			return defaultValue;
		}

		return Integer.parseInt(
			value
		);
	}
}