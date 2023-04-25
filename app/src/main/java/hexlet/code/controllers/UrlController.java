package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class UrlController {
    public static Handler listOfUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;
        int offset = (page - 1) * rowsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        ctx.attribute("urls", urls);
        ctx.attribute("page", page);
        ctx.render("urls.html");
    };

    public static Handler createUrl = ctx -> {
        String parsedUrl = parseUrl(ctx.formParam("url"));
        if (parsedUrl == null) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
        } else {
            Url url = new QUrl()
                    .name.equalTo(parsedUrl)
                    .findOne();
            if (url == null) {
                url = new Url(parsedUrl);
                url.save();
                ctx.sessionAttribute("flash", "Страница успешно добавлена");
                ctx.sessionAttribute("flash-type", "success");
            } else {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.sessionAttribute("flash-type", "danger");
            }
            ctx.redirect("/urls");
        }
    };

    public static String parseUrl(String transmittedUrl) {
        try {
            URL url = new URL(transmittedUrl);
            String urlProtocol = url.getProtocol();
            String urlAuthority = url.getAuthority();
            return urlProtocol + "://" + urlAuthority;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        try {
            if (url == null) {
                throw new NotFoundResponse();
            }
            String urlName = url.getName();
            HttpResponse<String> response = Unirest.get(urlName).asString();

            String context = response.getBody();
            Document doc = Jsoup.parse(context);

            int statusCode = response.getStatus();
            String title = doc.title();
            String h1 = "";
            String description = "";

            Element h1Element = doc.selectFirst("h1");
            Element descriptionElement = doc.selectFirst("meta[name=description]");

            if (h1Element != null) {
                h1 = h1Element.text();
            }
            if (descriptionElement != null) {
                description = descriptionElement.attr("content");
            }

            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        } finally {
            ctx.render("/urls/" + id);
        }
    };
}
