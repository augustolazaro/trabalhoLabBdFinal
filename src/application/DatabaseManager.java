package application;

import java.sql.* ;  // for standard JDBC programs
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.CustomerCountryStateModel;
import model.CustomerShoppingModel;
import model.Person;
import model.ProductSalesCategoryModel;
import model.SalesMonthModel;
import model.TaxesPaidModel;

public class DatabaseManager {
	private final String username = "r7962006";
	private final String password = "r7962006";
	public Connection connection = null;
	public Integer maxRowsNumber = 50;

	public void openConnection(){
		try {
			//Conecta com driver
			Class.forName("oracle.jdbc.driver.OracleDriver");
			//Conecta com banco
			connection = DriverManager.getConnection(
					"jdbc:oracle:thin:@grad.icmc.usp.br:15215:orcl",
					username,
					password);

		} catch (ClassNotFoundException e) {
			System.out.println("Driver n�o encontrado");
			e.printStackTrace();
			return;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Erro na conex�o com o banco");
			e.printStackTrace();
			return;
		}

		System.out.println("Conectado com o banco");
	}

	public ResultSet select(String str){
		System.out.println("cmd: " + str);
		try {
			Statement stmt;
			stmt = connection.createStatement();
			return stmt.executeQuery(str);
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}

	public ArrayList<Person> getPeopleArray(Integer limit){
		try {
			ArrayList<Person> list = new ArrayList<Person>();
			String str = "SELECT nome, sobrenome FROM Pessoa where ROWNUM <= ";
			str = str.concat(limit.toString());
			ResultSet rs = select(str);
			while(rs.next()){
				Person p = new Person(rs.getString("nome"), rs.getString("sobrenome"));
				list.add(p);
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}

	public ArrayList<CustomerShoppingModel> getCsmArray(String name, String clientId){
		String filter = " ";

		if (name != null && !name.isEmpty()){
			filter = "WHERE P.NOME = '" + name+"' "; 
		}
		if (clientId != null && !clientId.isEmpty()){
			filter = filter.concat(" OR P.PESSOA_ID = '"+clientId+"' ");
		}


		String query = 
				"SELECT P.NOME AS Nome,  " +
						"TO_CHAR(V.data_venda, 'YYYY') AS Ano, COUNT(*) AS Compras, " +
						"SUM(V.subtotal) AS TotalComprado " +
						"FROM Venda V " +
						"JOIN Cliente C " + 
						"ON C.cliente_id = V.cliente_id "+ 
						"JOIN Pessoa P " +
						"ON P.pessoa_id = C.pessoa_id " +
						filter +
						"AND ROWNUM <= " + maxRowsNumber +
						" GROUP BY ROLLUP (P.NOME, TO_CHAR(V.data_venda, 'YYYY')) " +
						"ORDER BY P.NOME";

		try {
			ArrayList<CustomerShoppingModel> list = new ArrayList<CustomerShoppingModel>();
			ResultSet rs = select(query);
			while(rs.next()){
				CustomerShoppingModel csm = 
						new CustomerShoppingModel(
								rs.getString("Nome"),
								rs.getString("Nome"), //TROCAR PARA CODIGO
								rs.getDouble("TotalComprado"),
								rs.getInt("Compras"),
								rs.getInt("Ano"));

				list.add(csm);	
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}

	public ArrayList<ProductSalesCategoryModel> getPscArray(String year){
		String filter = " ";
		if(year != null && !year.isEmpty()){
			filter = "WHERE data_venda LIKE '%/"+year+"' AND ";
		}else{
			filter = "WHERE ";
		}
		
		String query = 
				"SELECT Sub.categoria, " +
						"SUB.NOME AS subcategoria, " +
						"P.NOME AS produto, " +
						"TO_CHAR(V.data_venda, 'MM/YY') AS data_venda, " +
						"COUNT(*) AS unidade, " +
						"SUM(V.subtotal) AS total_vendido, " +
						"DENSE_rank() over (PARTITION BY SUB.CATEGORIA  order by sum(v.subtotal) ) as rank_categoria, " +
						"DENSE_rank() over (partition by p.nome order by sum(v.subtotal) ) as rank_subcategoria, " +
						"DENSE_rank() over (order by sum(v.subtotal)) AS rank_geral " +
						"FROM Venda V " +
						"JOIN Venda_item VI " +
						"ON V.venda_id = VI.venda_id " +
						"JOIN Produto P " +
						"ON P.produto_id = VI.produto_id " +
						"JOIN Subcategoria Sub " +
						"ON P.subcategoria = Sub.subcategoria_id " +
						filter +
						"ROWNUM <= " + maxRowsNumber +
						" GROUP BY ROLLUP(Sub.categoria, Sub.NOME, P.NOME, TO_CHAR(V.data_venda, 'MM/YY')) " +
						"ORDER BY Sub.CATEGORIA, sub.nome , p.nome, TO_CHAR(V.data_venda, 'MM/YY')";

		try {
			ArrayList<ProductSalesCategoryModel> list = new ArrayList<ProductSalesCategoryModel>();
			ResultSet rs = select(query);
			while(rs.next()){
				ProductSalesCategoryModel psc = 
						new ProductSalesCategoryModel(
								rs.getString("categoria"),
								rs.getString("subcategoria"),
								rs.getString("produto"),
								rs.getString("data_venda"),
								rs.getInt("unidade"),
								rs.getFloat("total_vendido"),
								rs.getInt("rank_categoria"),
								rs.getInt("rank_subcategoria"),
								rs.getInt("rank_geral"));

				list.add(psc);
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}

	public ArrayList<TaxesPaidModel> getTpArray(String year){
		String filter = " ";
		if(year != null && !year.isEmpty()){
			filter = "WHERE data_venda LIKE '%/"+year+"' AND ";
		}else{
			filter = "WHERE ";
		}
		
		String query = 
				"SELECT E.nome AS estado, TO_CHAR(V.data_venda, 'MM/YY') AS mes, SUM(I.taxa) AS total_imposto " +
						"FROM Venda V " +
						"JOIN Endereco En " +
						"ON V.ENDERECO_ENTREGA = En.endereco_id " +
						"JOIN Estado E " +
						"ON En.estado_id = E.estado_id " +
						"JOIN Imposto I " +
						"ON E.estado_id = I.estado_id " +
						filter +
						"ROWNUM <= " + maxRowsNumber +
						" GROUP BY ROLLUP(E.nome, TO_CHAR(V.data_venda, 'MM/YY')) " +
						"ORDER BY E.nome, TO_CHAR(V.data_venda, 'MM/YY')";

		try {
			ArrayList<TaxesPaidModel> list = new ArrayList<TaxesPaidModel>();
			ResultSet rs = select(query);
			while(rs.next()){
				TaxesPaidModel tp = 
						new TaxesPaidModel(
								rs.getString("estado"),
								rs.getString("mes"),
								rs.getDouble("total_imposto"));

				list.add(tp);
				System.out.println("ok");
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}
	
	public ArrayList<CustomerCountryStateModel> getCcsArray(){

		String query = 
				"SELECT EST.PAIS AS Pais, EST.NOME AS Estado ,COUNT(C.CLIENTE_ID) as Clientes "+
						"FROM CLIENTE C "+
						"JOIN PESSOA P "+
						"ON C.PESSOA_ID = P.PESSOA_ID "+
						"JOIN ENDERECO ENDE "+
						"ON P.PESSOA_ID = ENDE.PESSOA_ID "+
						"JOIN ESTADO EST "+
						"ON ENDE.ESTADO_ID = EST.ESTADO_ID "+
						"GROUP BY ROLLUP (EST.PAIS, EST.NOME) "+
						"ORDER BY EST.PAIS, EST.NOME";

		try {
			ArrayList<CustomerCountryStateModel> list = new ArrayList<CustomerCountryStateModel>();
			ResultSet rs = select(query);
			while(rs.next()){
				CustomerCountryStateModel csm = 
						new CustomerCountryStateModel(
								rs.getString("Pais"),
								rs.getString("Estado"),
								rs.getInt("Clientes"));


				list.add(csm);
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}

	public ArrayList<SalesMonthModel> getSmArray(String mes){
		String filter = "";
		if(mes != null && !mes.isEmpty()){
			filter = "WHERE DATA_VENDA LIKE '%/"+mes+"/12' AND ";
		}else{
			filter = "WHERE ";
		}

		String query = 
				"SELECT SOMA_SUBTOTAL.DIA AS Dia, SOMA_SUBTOTAL.VENDAS_DIA AS VendasDia, "+
						"TRUNC(avg (SOMA_SUBTOTAL.VENDAS_DIA) OVER " +
						"(ORDER BY SOMA_SUBTOTAL.DIA ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING),5) AS Media "+
						"FROM ( "+
						"SELECT "+
						"TO_CHAR(data_venda, 'DD/MM/YY') as dia, "+
						"SUM(VENDA.TOTAL_DEVIDO) AS VENDAS_DIA "+
						"FROM VENDA "+
						filter+
						"ROWNUM <= " + maxRowsNumber +
						" GROUP BY (TO_CHAR(data_venda, 'MM/YY'), TO_CHAR(data_venda, 'DD/MM/YY'))) SOMA_SUBTOTAL";

		try {
			ArrayList<SalesMonthModel> list = new ArrayList<SalesMonthModel>();
			ResultSet rs = select(query);
			while(rs.next()){
				SalesMonthModel csm = 
						new SalesMonthModel(
								rs.getString("Dia"),
								rs.getInt("VendasDia"),
								rs.getDouble("Media"));


				list.add(csm);
			}
			return list;
		} catch (SQLException ex) {
			System.out.println(ex);
			return null;
		}
	}


	public boolean closeConnection(){
		try {
			connection.close();
			System.out.println("Connection closed");
		} catch (SQLException ex) {
			System.out.println("Failed to close connection");
			return false;
		}
		return true;
	}
}
