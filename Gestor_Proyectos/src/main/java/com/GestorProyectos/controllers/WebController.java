package com.GestorProyectos.controllers;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.entity.Consulta;
import com.google.code.stackexchange.client.query.QuestionApiQuery;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.Paging;
import com.google.code.stackexchange.schema.Question;
import com.google.code.stackexchange.schema.StackExchangeSite;

@Controller
public class WebController {

	@Value("${api.github.token}")
	private String githubToken;

	@Value("${api.github.username}")
	private String githubUsername;

	@Value("${api.gitlab.token}")
	private String gitlabToken;

	@Value("${api.stackoverflow.key}")
	private String stackoverflowKey;

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
		text.append("Numero de visitante");
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
				text.append(consulta.getNumeroVisitante());
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
		client.setOAuth2Token(githubToken);
		client.getUser();
		client.setCredentials(githubUsername, githubToken);
		RepositoryService service = new RepositoryService(client);
		System.out.println("hola mundo4");
		// List<SearchRepository> search=service.searchRepositories("labh");
		for (int i = 1; i <= 1; i++) {
			for (SearchRepository repo : service.searchRepositories(nombre, i))
				// System.out.println(repo.getName() +" Nombre author "+repo.getOwner()+ "
				// Watchers: " + repo.getWatchers());
				consultas.add(new Consulta(repo.getId(),repo.getName(),repo.getOwner(),repo.getWatchers()));
			
			}
	}
	private void obtenerConsultasGitlab(String nombre, List<Consulta> consultas) {
		//Consulta de gitlab YYfuSdapvSjuMQrzzWSy
	    @SuppressWarnings("resource")
		GitLabApi gitLabApi = new GitLabApi("https://gitlab.com/", gitlabToken);
	    List<Project> projectPager;
		try {
			for(int i = 0; i <= 1; i++) {
			projectPager = gitLabApi.getProjectApi().getProjects(nombre,i,100);
			for (Project project : projectPager) {
				consultas.add(new Consulta(String.valueOf(project.getId()),project.getName(),project.getNamespace().getName(),project.getStarCount()));
			}
			}
		} catch (GitLabApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void obtenerConsultasStackOverflow(String nombre, List<Consulta> consultas) {
		StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory.newInstance(stackoverflowKey, StackExchangeSite.STACK_OVERFLOW);
		QuestionApiQuery query2 = queryFactory.newQuestionApiQuery();
		for(int i = 0; i <= 1; i++) {
	    List<Question> questions3 = query2.withSort(Question.SortOrder.MOST_VOTED).withPaging(new Paging(i, 100)).withTags(nombre).list();
	    for(Question q:questions3) {
			consultas.add(new Consulta(String.valueOf(q.getOwner().getUserId()),q.getTitle(),q.getOwner().getDisplayName(),q.getViewCount()));
	    	}
		}
	}
	@SuppressWarnings("unchecked")
	private void obtenerConsultasBitbucket(String nombre, List<Consulta> consultas, int maxnumber) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			String apiUrl = "https://api.bitbucket.org/2.0/repositories?q=name~%22" + nombre + "%22&pagelen=" + maxnumber;
			Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);
			if (response != null && response.containsKey("values")) {
				List<Map<String, Object>> repos = (List<Map<String, Object>>) response.get("values");
				for (Map<String, Object> repo : repos) {
					String fullName = (String) repo.get("full_name");
					String name = (String) repo.get("name");
					String owner = fullName != null ? fullName.split("/")[0] : "";
					String id = fullName != null ? fullName : String.valueOf(consultas.size());
					consultas.add(new Consulta(id, name, owner, 0));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
