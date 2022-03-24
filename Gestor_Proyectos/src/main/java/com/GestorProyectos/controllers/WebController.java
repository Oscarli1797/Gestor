package com.GestorProyectos.controllers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.entity.Consulta;
import com.google.code.stackexchange.client.query.QuestionApiQuery;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.Paging;
import com.google.code.stackexchange.schema.Question;
import com.google.code.stackexchange.schema.StackExchangeSite;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class WebController {


	@Autowired
	private RedisUtils redisUtils;
	private Long redisExpire = (long) (120 * 10000);

	@RequestMapping(value = "/buscador")
	public String indexchange(Model model,HttpSession usuario,HttpServletResponse response, @RequestParam int valor, @RequestParam String nombre) throws IOException {
		if (usuario.getAttribute("registered") == null) {
			usuario.setAttribute("registered", false);

		}
		if (usuario.getAttribute("admin") == null) {
			model.addAttribute("noadmin", true);
		} else {
			model.addAttribute("admin", usuario.getAttribute("admin"));
		}
		model.addAttribute("registered", usuario.getAttribute("registered"));

		boolean aux = !(Boolean) usuario.getAttribute("registered");
		model.addAttribute("unregistered", aux);
		if(redisUtils.exists(valor+nombre)) {
			 @SuppressWarnings("unchecked")
			List<Consulta> consultas = (List<Consulta>) redisUtils.get(valor+nombre);
			 if(consultas.size()>10) {
					model.addAttribute("lista", consultas.subList(0, 10));
				}else {
					model.addAttribute("lista", consultas);
				}
				model.addAttribute("consulta", true);
				usuario.setAttribute("clave", valor+nombre);
				return "Index";
		}else {
			List<Consulta> consultas = new ArrayList<Consulta>();	
			switch(valor) {
			case 1:
				obtenerConsultasGithub(nombre,consultas);
				break;
			case 2:
				obtenerConsultasGitlab(nombre,consultas);
				break;
			case 3:
				obtenerConsultasStackOverflow(nombre,consultas);
				break;
			case 4:
				obtenerConsultasBitbucket(nombre,consultas,10);
				break;
			}		
			if(consultas.size()>10) {
				model.addAttribute("lista", consultas.subList(0, 10));
			}else {
				model.addAttribute("lista", consultas);
			}
			model.addAttribute("consulta", true);
			redisUtils.set(valor+nombre, consultas,redisExpire);
			return "Index";
		}
		 
	}

	@RequestMapping(value = "/search")
	public String index(Model model, HttpServletRequest request, HttpSession usuario) {
		if (usuario.getAttribute("registered") == null) {
			usuario.setAttribute("registered", false);

		}
		if (usuario.getAttribute("admin") == null) {
			model.addAttribute("noadmin", true);
		} else {
			model.addAttribute("admin", usuario.getAttribute("admin"));
		}
		model.addAttribute("registered", usuario.getAttribute("registered"));

		boolean aux = !(Boolean) usuario.getAttribute("registered");
		model.addAttribute("unregistered", aux);
		return "search";
	}

	@RequestMapping("/exporttext")
	public void exportConsulta(HttpServletResponse response, HttpSession usuario) {
		StringBuffer text = new StringBuffer();
		text.append("Id");
		text.append("   |    ");
		text.append("Titulo");
		text.append("   |    ");
		text.append("Autor");
		text.append("   |    ");
		text.append("Url");
		text.append("\r\n");
		String 	clave=(String) usuario.getAttribute("clave");
		if(redisUtils.exists(clave)) {
			@SuppressWarnings("unchecked")
			List<Consulta> consultas = (List<Consulta>) redisUtils.get(clave);
			for (Consulta consulta : consultas) {
				text.append(consulta.getIdConsulta());
				text.append("   |    ");
				text.append(consulta.getNombre());
				text.append("   |    ");
				text.append(consulta.getAutor());
				text.append("   |    ");
				text.append(consulta.geturl());
				text.append("\r\n");
			}
			exportTxt(response, text.toString());
		}
	}

	public void exportTxt(HttpServletResponse response,String text){
		response.setCharacterEncoding("utf-8");
		response.setContentType("text/plain");
		response.addHeader("Content-Disposition","attachment;filename="+ genAttachmentFileName( "Lista_Consulta", "consulta")+ ".txt");
		BufferedOutputStream buff = null;
		ServletOutputStream outStr=null;
		 try {
			 outStr = response.getOutputStream();
			 buff = new BufferedOutputStream(outStr);
			 buff.write(text.getBytes("UTF-8"));
			 buff.flush();
			 buff.close();
			} catch (Exception e) {
				System.out.println("Error en exportar fichero");
			} finally{
				try {
					buff.close();
	                outStr.close();
				}catch (Exception e) {
	            }
			}
	}

	public String genAttachmentFileName(String fileName, String defaultName) {
		try {
			fileName = new String(fileName.getBytes("gb2312"), "ISO8859-1");
		} catch (Exception e) {
			fileName = defaultName;
		}
		return fileName;
	}

	private void obtenerConsultasGithub(String nombre, List<Consulta> consultas) throws IOException {
		GitHubClient client = new GitHubClient();
		client.setOAuth2Token("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");
		client.getUser();
		client.setCredentials("oscarli1797", "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");
		RepositoryService service = new RepositoryService(client);
		System.out.println("hola mundo4");
		// List<SearchRepository> search=service.searchRepositories("labh");
		for (int i = 1; i <= 1; i++) {
			for (SearchRepository repo : service.searchRepositories(nombre, i))
				// System.out.println(repo.getName() +" Nombre author "+repo.getOwner()+ "
				// Watchers: " + repo.getWatchers());
				consultas.add(new Consulta(repo.getId(),repo.getName(),repo.getOwner(),repo.getUrl()));
			
			}
	}
	private void obtenerConsultasGitlab(String nombre, List<Consulta> consultas) {
		//Consulta de gitlab YYfuSdapvSjuMQrzzWSy
	    @SuppressWarnings("resource")
		GitLabApi gitLabApi = new GitLabApi("https://gitlab.com/", "glpat-Tx2qTcVntkyWYqkA9wzP");
	    List<Project> projectPager;
		try {
			for(int i = 0; i <= 1; i++) {
			projectPager = gitLabApi.getProjectApi().getProjects(nombre,i,100);
			for (Project project : projectPager) {
				consultas.add(new Consulta(String.valueOf(project.getId()),project.getName(),project.getNamespace().getName(),project.getWebUrl()));
			}
			}
		} catch (GitLabApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void obtenerConsultasStackOverflow(String nombre, List<Consulta> consultas) {
		StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance("FlCUxvCHHyLU)oJ0kOsgRA((",StackExchangeSite.STACK_OVERFLOW);
		QuestionApiQuery query2 = queryFactory.newQuestionApiQuery();
		for(int i = 0; i <= 1; i++) {
	    List<Question> questions3 = query2.withSort(Question.SortOrder.MOST_VOTED).withPaging(new Paging(i, 100)).withTags(nombre).list();
	    for(Question q:questions3) {
			consultas.add(new Consulta(String.valueOf(q.getOwner().getUserId()),q.getTitle(),q.getOwner().getDisplayName(),q.getQuestionAnswersUrl()!=null?q.getQuestionAnswersUrl():""));
	    	}
		}
	}
	private void obtenerConsultasBitbucket(String nombre,List<Consulta> consultas, int maxnumber) {
		int i=1;
		boolean seguir=true;
		  while(seguir && i<maxnumber) {
		  seguir=false;
		  String get=SendGet(nombre,i);
		  if(get.contains("next-page-link")) {
		  		i++;
		  		seguir=true;
		  	}
		  	consultas.addAll(RegexString(get));
		  }
	}
	public static String SendGet(String nombre,int i) {
	    String url = "https://bitbucket.org/repo/all/"+i+"?name=";

	      // Definir un string para almacenar el resultado de url 
	      String result = "";  
	      BufferedReader in = null;  
	      try {   
	             // Conseguir el url real   
	             URL realUrl = new URL(url+nombre.replace(" ", "+"));
	             // iniciar la coneccion para url real 
	             URLConnection connection = realUrl.openConnection();   
	             // Empezar la coneccion   
	             connection.connect();
	             // iniciar BufferedReader para tener la respuesta de coneccion   
	             in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
	             // String que almacena la linea de bufferreader 
	             String line;   
	             while ((line = in.readLine()) != null) {    
	                 result += line; 
	                 result +=("\r\n");
	             }
	          } catch (Exception e) {   
	                 System.out.println("Error por el get url de bicbucket！" + e);   
	                 e.printStackTrace();  
	            }  
	           // usar finally para cerrar el buffer
	          finally {   
	                 try {    
	                         if (in != null) {     
	                             in.close();
	                         }
	                     } catch (Exception e2) {    
	                         e2.printStackTrace();
	                     }
	                 }
	      return result;
	         }
	  public static ArrayList<Consulta>  RegexString(String getcontent) {  
	      ArrayList<Consulta> results=new ArrayList<Consulta>();
	      // pattern para encontrar el titulo del repositorio 
	      Pattern pattern = Pattern.compile("avatar-link.+?href=\\\"(.+?)\\\"");  
	      Matcher matcher = pattern.matcher(getcontent); 
	      //bolean para saber si encuentras 
	      boolean isFind=matcher.find();
	      
	      while(isFind) {
	    	  Consulta consulta=new Consulta();
	    	  consulta.setContent(matcher.group(1));
	          results.add(consulta);
	          isFind=matcher.find(); 
	     }  
	      return results;
	  }  
}
