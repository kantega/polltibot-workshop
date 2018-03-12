package no.kantega.polltibot.twitter;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;
import fj.F;
import fj.data.List;
import org.kantega.kson.json.JsonObject;
import org.kantega.kson.parser.JsonParser;
import org.kantega.niagara.Source;
import org.kantega.niagara.Sources;
import org.kantega.niagara.Task;

import java.time.Duration;


public class TwitterClient {

    static final F<String,String> statuses = id->"https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name="+id+"&include_rts=1&count=200";
    static final List<String> politiaccounts =  List.arrayList("PolitiTrondelag");//,"oslopolitiops","polititroms","politifinnmark");

    static final String q = "politi";
    static final String queryUrl = "https://api.twitter.com/1.1/search/tweets.json?lanf=no&q=" + q;

    public static Source<JsonObject> tweets(String apiKey, String apiSecret, String token, String tokenSecret) {
        try {

            final OAuth10aService service =
                    new ServiceBuilder(apiKey)
                            .apiSecret(apiSecret)
                            .callback("oob")
                            .build(TwitterApi.instance());

/*
            final OAuth1RequestToken requestToken =
                    service.getRequestToken();

            final OAuth1AccessToken accessToken =
                    service.getAccessToken(requestToken, "verifier you got from the user/callback");
*/
            long maxId = 0;

            //final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/search/tweets.json?q=" + q);

            Source<List<JsonObject>> lists = Sources.callback(cb -> {
                for(String id:politiaccounts) {
                    long nextMax = maxId;
                    for (int i = 0; i < 30; i++) {
                        System.out.println("Reading tweets with max " + nextMax);
                        List<JsonObject> tweets = parseStatuses.f(
                                getRequest(id, service, nextMax, token, tokenSecret).executeAndAwait(Duration.ofSeconds(30)).orThrow()
                        );
                        cb.handle(tweets).executeAndAwait(Duration.ofSeconds(30));
                        nextMax = tweets.reverse().head().fieldAsLong("id").orThrow();
                    }
                }
            });

            return lists.flatten(l -> l);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    static final String uri(long maxId, String account){
        return "https://api.twitter.com/1.1/statuses/user_timeline.json?screen_name="+account+"&include_rts=1&count=200"+ (maxId == 0 ? "" : "&max_id=" + maxId);
    }

    static Task<String> getRequest(String id, OAuth10aService service, long maxId, String token, String tokenSecret) {
        return Task.tryTask(() -> {
            OAuthRequest request =
                    new OAuthRequest(Verb.GET, uri(maxId,id));

            service.signRequest(new OAuth1AccessToken(token, tokenSecret), request); // the access token from step 4
            final Response response = service.execute(request);


            return response.getBody();
        });
    }

    static F<String, List<JsonObject>> parseStatuses =
            body -> JsonParser.parse(body).asArray().mapArray(jsonValue -> jsonValue.asObject().orThrow()).orThrow();


}
