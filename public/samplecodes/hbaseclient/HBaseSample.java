package hbaseclient;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.io.compress.Compression.Algorithm;
import org.apache.hadoop.hbase.util.Bytes;

public class HBaseSample {
  private static final String TABLE_NAME = "MY_TABLE_NAME_TOO";
  private static final String CF_DEFAULT = "DEFAULT_COLUMN_FAMILY";

  public static void createOrOverwrite(Admin admin, HTableDescriptor table) throws IOException {
    if (admin.tableExists(table.getTableName())) {
      admin.disableTable(table.getTableName());
      admin.deleteTable(table.getTableName());
    }
    admin.createTable(table);
  }

  public static void createSchemaTables(Configuration config) throws IOException {
    Connection connection = ConnectionFactory.createConnection(config);
    Admin admin = connection.getAdmin();
    HTableDescriptor table = new HTableDescriptor(TableName.valueOf(TABLE_NAME));
    table.addFamily(new HColumnDescriptor(CF_DEFAULT).setCompressionType(Algorithm.NONE));
    table.addFamily(new HColumnDescriptor(CF_DEFAULT + 2).setCompressionType(Algorithm.NONE));
    System.out.print("Creating table. ");

    createOrOverwrite(admin, table);
    System.out.println("Table is created.");
  }

  public static void modifySchema(Configuration config) throws IOException {
    System.out.println("start to modifySchema ......");
    Connection connection = ConnectionFactory.createConnection(config);
    Admin admin = connection.getAdmin();

    TableName tableName = TableName.valueOf(TABLE_NAME);
    if (!admin.tableExists(tableName)) {
      System.out.println("Table does not exist.");
      System.exit(-1);
    }

    HTableDescriptor table = admin.getTableDescriptor(tableName);

    // Update existing table
    HColumnDescriptor newColumn = new HColumnDescriptor("NEWCF");
    newColumn.setCompactionCompressionType(Algorithm.GZ);
    newColumn.setMaxVersions(HConstants.ALL_VERSIONS);
    admin.addColumn(tableName, newColumn);
    table.addFamily(newColumn);

    // Update existing column family
    HColumnDescriptor existingColumn = new HColumnDescriptor(CF_DEFAULT);
    existingColumn.setCompactionCompressionType(Algorithm.GZ);
    existingColumn.setMaxVersions(HConstants.ALL_VERSIONS);
    table.modifyFamily(existingColumn);
    admin.modifyTable(tableName, table);
    System.out.println("end to modifySchema ......");
  }

  public static void drop(Configuration config) throws IOException {
    Connection connection = ConnectionFactory.createConnection(config);
    Admin admin = connection.getAdmin();
    TableName tableName = TableName.valueOf(TABLE_NAME);
    if (!admin.tableExists(tableName)) {
      System.out.println("Table does not exist.");
      System.exit(-1);
    }
    // Disable an existing table
    admin.disableTable(tableName);

    // Delete an existing column family
    admin.deleteColumn(tableName, CF_DEFAULT.getBytes("UTF-8"));

    // Delete a table (Need to be disabled first)
    admin.deleteTable(tableName);
  }

  private static void insertData(Configuration config) throws IOException {
    System.out.println("start to insert data ......");
    Connection connection = ConnectionFactory.createConnection(config);
    Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
    Put put = new Put("112233bbbcccc".getBytes());
    put.addColumn((CF_DEFAULT).getBytes(), "aaaaaa".getBytes(), "aaa".getBytes());
    put.addColumn((CF_DEFAULT + 2).getBytes(), "bbbbb".getBytes(), "bbbb".getBytes());
    put.addColumn("NEWCF".getBytes(), "testcccc".getBytes(), "testcccccc".getBytes());
    try {
      table.put(put);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("end to insert data ......");
  }

  private static void queryData(Configuration config) throws IOException {
    System.out.println("start to query data ......");
    Connection connection = ConnectionFactory.createConnection(config);
    Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
    try {
      // Use the table as needed, for a single operation and a single thread
      ResultScanner rs = table.getScanner(new Scan());
      for (Result r : rs) {
        System.out.println("rowkey:" + new String(r.getRow()));
        Cell[] rawCells = r.rawCells();
        for (Cell cell : rawCells) {
          System.out.println("Rowkey:" + Bytes.toString(CellUtil.cloneRow(cell)) + "   Familiy:Quilifier:"
              + Bytes.toString(CellUtil.cloneFamily(cell)) + ":" + Bytes.toString(CellUtil.cloneQualifier(cell))
              + "   Value: " + Bytes.toString(CellUtil.cloneValue(cell)) + "   Time: " + cell.getTimestamp());
        }
      }
    } finally {
      table.close();
      connection.close();
    }
    System.out.println("end to query data ......");
  }

  private static void changeData(Configuration config) throws IOException {
    System.out.println("start to update data ......");
    Connection connection = ConnectionFactory.createConnection(config);
    Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
    Put put = new Put("112233bbbcccc".getBytes());
    put.addColumn((CF_DEFAULT + 2).getBytes(), "aaaaaa".getBytes(), "aaa---new---".getBytes());
    put.addColumn((CF_DEFAULT + 2).getBytes(), "bbbbb".getBytes(), null);
    try {
      table.put(put);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("end to update data ......");
  }

  private static void deleteData(Configuration config) throws IOException {
    System.out.println("start to delete data ......");
    Connection connection = ConnectionFactory.createConnection(config);
    Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
    Delete delete = new Delete("112233bbbcccc".getBytes());
    // delete.addColumn((CF_DEFAULT + 2).getBytes(), null);
    try {
      table.delete(delete);
    } catch (IOException e) {
      e.printStackTrace();
    }
    System.out.println("end to delete data ......");
  }

  public static void main(String... args) throws IOException {
    Configuration config = HBaseConfiguration.create();

    // Add any necessary configuration files (hbase-site.xml, core-site.xml)
    config.addResource(new Path("/Users/xiningwang/hadoop/hbase-1.2.5/conf", "hbase-site.xml"));
    config.addResource(new Path("/Users/xiningwang/hadoop/hadoop-2.7.3/etc/hadoop", "core-site.xml"));
    createSchemaTables(config);
    modifySchema(config);
    
    insertData(config);
    queryData(config);
    changeData(config);
    queryData(config);
    deleteData(config);
    queryData(config);

    drop(config);
    System.out.println("---completed......");
  }

}
