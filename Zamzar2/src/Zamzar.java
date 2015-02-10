import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Zamzar {

	public static String API_KEY = "82c8ba0c82f0256665008126efef78066a7a9ef9";

	public static String SERVIDOR_ZAMZAR = "https://api.zamzar.com/v1";

	private static CloseableHttpClient getHttpClient() {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials(API_KEY, ""));

		CloseableHttpClient httpClient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(credentialsProvider).build();

		return httpClient;
	}

	// Mostra todos os formatos suportados para conversao.
	public static String getAvailableFormats() throws IOException {
		String formatsEndpoint = SERVIDOR_ZAMZAR + "/formats";

		CloseableHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(formatsEndpoint);

		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity responseContent = response.getEntity();
		String resultado = EntityUtils.toString(responseContent, "UTF-8");

		response.close();
		httpClient.close();

		return resultado;
	}

	public static String enviarEConverterArquivo(File caminhoArquivo,
			String formato) throws IOException {
		String uploadEndpoint = SERVIDOR_ZAMZAR + "/jobs";

		CloseableHttpClient httpClient = getHttpClient();
		FileBody fileContent = new FileBody(caminhoArquivo);
		StringBody header = new StringBody(formato, ContentType.TEXT_PLAIN);

		HttpEntity reqEntity = MultipartEntityBuilder.create()
				.addPart("source_file", fileContent)
				.addPart("target_format", header).build();

		HttpPost httpPost = new HttpPost(uploadEndpoint);
		httpPost.setEntity(reqEntity);

		CloseableHttpResponse response = httpClient.execute(httpPost);
		HttpEntity responseContent = response.getEntity();
		String resultado = EntityUtils.toString(responseContent, "UTF-8");

		response.close();
		httpClient.close();

		return resultado;
	}

	/**
	 * Method is used for polling of a conversion process. Conversion process
	 * might be time consuming. To get an accurate state of the conversion
	 * process, use this method.
	 * 
	 * @param jobId
	 *            Id of a job that was assigned to a process by calling
	 *            "uploadAndConvertFile" method
	 * @return Text output in a format of JSON. JSON data with a status of a
	 *         conversion process
	 * @throws IOException
	 *             May occur during opening or closing a connection with the
	 *             remote server.
	 */
	public static String pollJob(String jobId) throws IOException {
		String pollEndpoint = SERVIDOR_ZAMZAR + "/jobs/" + jobId;

		CloseableHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(pollEndpoint);

		CloseableHttpResponse response = httpClient.execute(httpGet);
		
		HttpEntity responseContent = response.getEntity();
		String result = EntityUtils.toString(responseContent, "UTF-8");

		response.close();
		httpClient.close();

		try{  
		      Thread.sleep(3000);  
		}catch(Exception e){  
		      System.out.println("Deu erro!");  
		} 
		
		return result;

	}

	public static String salvarArquivoConvertido(String idArquivo,
			File localArquivo) throws IOException {
		String fileSaveEndpoint = SERVIDOR_ZAMZAR + "/files/" + idArquivo
				+ "/content";

		CloseableHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(fileSaveEndpoint);

		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity responseContent = response.getEntity();

		BufferedInputStream bis = new BufferedInputStream(
				responseContent.getContent());
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(localArquivo));
		int inByte;
		while ((inByte = bis.read()) != -1) {
			bos.write(inByte);
		}

		response.close();
		httpClient.close();
		bis.close();
		bos.close();

		return "Convertido com sucesso!";
	}

	public static String uploadArquivoParaServidor(File caminhoArquivo,
			String nome) throws IOException {
		String uploadEndpoint = SERVIDOR_ZAMZAR + "/files";

		CloseableHttpClient httpClient = getHttpClient();
		FileBody fileContent = new FileBody(caminhoArquivo);
		StringBody header = new StringBody(nome, ContentType.TEXT_PLAIN);

		HttpEntity reqEntity = MultipartEntityBuilder.create()
				.addPart("content", fileContent).addPart("name", header)
				.build();

		HttpPost httpPost = new HttpPost(uploadEndpoint);
		httpPost.setEntity(reqEntity);

		CloseableHttpResponse response = httpClient.execute(httpPost);
		HttpEntity responseContent = response.getEntity();
		String resultado = EntityUtils.toString(responseContent, "UTF-8");

		response.close();
		httpClient.close();

		return resultado;
	}

	public static void main(String[] args) throws JSONException {
		try {

			String jsonArquivoConvertido = enviarEConverterArquivo(new File(
					"/Users/felipe/Desktop/Converter/F2AD14ING714-2.ppt"),
					"html");
			System.out.println(jsonArquivoConvertido);

			JSONObject ob = new JSONObject(jsonArquivoConvertido);
			String id = String.valueOf(ob.getInt("id"));

			String polledJob = pollJob(id);

			JSONObject obj = null;

			JSONObject objeto = new JSONObject(polledJob);

			JSONArray arrayArquivos = objeto.getJSONArray("target_files");

			for (int j = 0; j < arrayArquivos.length(); j++) {
				obj = arrayArquivos.getJSONObject(j);
			}

			// System.out.println("ID TARGET: " + obj.getInt("id"));
			JSONObject source_file = ob.getJSONObject("source_file");
			String nomeArquivo = source_file.getString("name");
			String saveStatus = salvarArquivoConvertido(String.valueOf(obj
					.getInt("id")), new File(
					"/Users/felipe/Desktop/Convertidos/" + nomeArquivo));
			System.out.println("---------");
			System.out.println("Status conversao: " + saveStatus);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
