package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    private static MockWebServer server;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    final void beforeEach() {
        transaction = DB.beginTransaction();
    }

    @AfterEach
    final void afterEach() {
        transaction.rollback();
    }

    @Test
    void testRootPage() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Test
    void testUrlsPage() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Test
    void testUrl() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/1").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("https://www.github.com");
    }

    @Test
    void testUrls() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("https://www.github.com");
        assertThat(response.getBody()).contains("https://twitter.com");
    }

    @Test
    void testIncorrectUrlAddition() {
        String incorrectUrl = "moon.com";
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", incorrectUrl)
                .asEmpty();

        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/");

        HttpResponse<String> responseWithIncorrectUrl = Unirest.get(baseUrl).asString();
        assertThat(responseWithIncorrectUrl.getBody()).contains("Некорректный URL");
    }

    @Test
    void testCorrectUrlAddition() {
        String url = "https://www.example.com";
        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url addedUrl = new QUrl()
                .name.equalTo(url)
                .findOne();

        assertThat(addedUrl).isNotNull();
        assertThat(addedUrl.getName()).isEqualTo(url);

        HttpResponse<String> newResponse = Unirest.get(baseUrl + "/urls").asString();
        assertThat(newResponse.getStatus()).isEqualTo(200);
        assertThat(newResponse.getBody()).contains(url);
        assertThat(newResponse.getBody()).contains("Страница успешно добавлена");
    }

    @Test
    void testExistingUrlAddition() {
        String url = "https://twitter.com";

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", url)
                .asString();

        HttpResponse<String> responseGet = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(responseGet.getStatus()).isEqualTo(200);
        assertThat(responseGet.getBody()).contains("Страница уже существует");
        assertThat(responseGet.getBody()).contains(url);
    }

    @Test
    void testShowUrl() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/2").asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("https://twitter.com");
        assertThat(response.getBody()).contains("description");
        assertThat(response.getBody()).contains("Запустить проверку");
    }

    @Test
    void testCheckUrl() throws Exception {
        server = new MockWebServer();
        String expectedBody = Files.readString(Path.of("src/test/resources/test.html"));
        server.enqueue(new MockResponse().setBody(expectedBody));
        server.start();
        String serverUrl = server.url("/").toString();

        HttpResponse<String> response = Unirest
                .post(baseUrl + "/urls")
                .field("url", serverUrl)
                .asEmpty();

        Url url = new QUrl()
                .name.equalTo(serverUrl.substring(0, serverUrl.length() - 1))
                .findOne();

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls/" + url.getId() + "/checks")
                .asString();

        assertThat(responsePost.getStatus()).isEqualTo(302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls/" + url.getId());

        String body = Unirest
                .get(baseUrl + "/urls/" + url.getId())
                .asString()
                .getBody();

        assertThat(body).contains("200");
        assertThat(body).contains("Хекслет");
        assertThat(body).contains("Живое онлайн сообщество");
        assertThat(body).contains("Это заголовок h1");
    }
}
