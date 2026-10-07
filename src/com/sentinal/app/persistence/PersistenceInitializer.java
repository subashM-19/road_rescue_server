package com.sentinal.app.persistence;

import com.sentinal.persistencefw.PersistenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class PersistenceInitializer
{
	private PersistenceInitializer()
	{
	}

	public static void initialize(
		String productRoot)
	{
		if (productRoot == null ||
			productRoot.isEmpty())
		{
			throw new IllegalArgumentException(
				"Product root cannot be null or empty"
			);
		}

		File persistenceDirectory =
			new File(
				productRoot,
				"product_package/conf/app/persistence"
			);

		File schemaFile =
			new File(
				persistenceDirectory,
				"schema.xml"
			);

		File databasePropertiesFile =
			new File(
				persistenceDirectory,
				"database.properties"
			);

		SchemaXMLParser schemaXMLParser =
			new SchemaXMLParser();

		schemaXMLParser.load(
			schemaFile
		);

		Properties properties =
			loadProperties(
				databasePropertiesFile
			);

		try
		{
			Class.forName("com.mysql.cj.jdbc.Driver");
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(
				"MySQL JDBC driver not found",
				e
			);
		}

		PersistenceManager.initialize(
			getRequiredProperty(
				properties,
				"url"
			),
			getRequiredProperty(
				properties,
				"username"
			),
			getRequiredProperty(
				properties,
				"password"
			)
		);
	}

	private static Properties loadProperties(
		File file)
	{
		if (!file.exists())
		{
			throw new IllegalArgumentException(
				"Database configuration does not exist: "
					+ file.getAbsolutePath()
			);
		}

		Properties properties =
			new Properties();

		try (FileInputStream inputStream =
				 new FileInputStream(file))
		{
			properties.load(
				inputStream
			);
		}
		catch (Exception e)
		{
			throw new IllegalStateException(
				"Unable to load database configuration from "
					+ file.getAbsolutePath(),
				e
			);
		}

		return properties;
	}

	private static String getRequiredProperty(
		Properties properties,
		String name)
	{
		String value =
			properties.getProperty(name);

		if (value == null ||
			value.trim().isEmpty())
		{
			return "";
		}

		return value.trim();
	}
}