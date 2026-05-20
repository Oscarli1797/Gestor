package com.GestorProyectos.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.entity.Consulta;
import com.google.code.stackexchange.client.query.QuestionApiQuery;
import com.google.code.stackexchange.client.query.StackExchangeApiQueryFactory;
import com.google.code.stackexchange.schema.Paging;
import com.google.code.stackexchange.schema.Question;
import com.google.code.stackexchange.schema.StackExchangeSite;

@Service
public class SearchService {

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

    private static final long CACHE_EXPIRE_SECONDS = 1200L;

    /**
     * Busca en la plataforma indicada. Usa Redis como caché.
     * @param platform 1=GitHub, 2=GitLab, 3=StackOverflow, 4=Bitbucket
     */
    @SuppressWarnings("unchecked")
    public List<Consulta> search(int platform, String nombre) {
        String cacheKey = platform + nombre;
        if (redisUtils.exists(cacheKey)) {
            return (List<Consulta>) redisUtils.get(cacheKey);
        }
        List<Consulta> results = fetchFromPlatform(platform, nombre);
        redisUtils.set(cacheKey, results, CACHE_EXPIRE_SECONDS);
        return results;
    }

    /** Recupera resultados cacheados por clave de sesión (usado para exportar). */
    @SuppressWarnings("unchecked")
    public List<Consulta> getCachedResults(String cacheKey) {
        if (cacheKey != null && redisUtils.exists(cacheKey)) {
            return (List<Consulta>) redisUtils.get(cacheKey);
        }
        return List.of();
    }

    private List<Consulta> fetchFromPlatform(int platform, String nombre) {
        List<Consulta> results = new ArrayList<>();
        switch (platform) {
            case 1 -> searchGithub(nombre, results);
            case 2 -> searchGitlab(nombre, results);
            case 3 -> searchStackOverflow(nombre, results);
            case 4 -> searchBitbucket(nombre, results, 10);
        }
        return results;
    }

    private void searchGithub(String nombre, List<Consulta> consultas) {
        try {
            GitHubClient client = new GitHubClient();
            client.setOAuth2Token(githubToken);
            client.setCredentials(githubUsername, githubToken);
            RepositoryService service = new RepositoryService(client);
            for (int i = 1; i <= 1; i++) {
                for (SearchRepository repo : service.searchRepositories(nombre, i)) {
                    consultas.add(new Consulta(repo.getId(), repo.getName(), repo.getOwner(), repo.getWatchers()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void searchGitlab(String nombre, List<Consulta> consultas) {
        try (GitLabApi gitLabApi = new GitLabApi("https://gitlab.com/", gitlabToken)) {
            for (int i = 0; i <= 1; i++) {
                List<Project> projects = gitLabApi.getProjectApi().getProjects(nombre, i, 100);
                for (Project project : projects) {
                    consultas.add(new Consulta(
                        String.valueOf(project.getId()),
                        project.getName(),
                        project.getNamespace().getName(),
                        project.getStarCount()
                    ));
                }
            }
        } catch (GitLabApiException e) {
            e.printStackTrace();
        }
    }

    private void searchStackOverflow(String nombre, List<Consulta> consultas) {
        StackExchangeApiQueryFactory queryFactory = StackExchangeApiQueryFactory
            .newInstance(stackoverflowKey, StackExchangeSite.STACK_OVERFLOW);
        QuestionApiQuery query = queryFactory.newQuestionApiQuery();
        for (int i = 0; i <= 1; i++) {
            List<Question> questions = query
                .withSort(Question.SortOrder.MOST_VOTED)
                .withPaging(new Paging(i, 100))
                .withTags(nombre)
                .list();
            for (Question q : questions) {
                consultas.add(new Consulta(
                    String.valueOf(q.getOwner().getUserId()),
                    q.getTitle(),
                    q.getOwner().getDisplayName(),
                    q.getViewCount()
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void searchBitbucket(String nombre, List<Consulta> consultas, int maxnumber) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://api.bitbucket.org/2.0/repositories?q=name~%22"
                + nombre + "%22&pagelen=" + maxnumber;
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
