package irish.franklin.springmssqljson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import irish.franklin.springmssqljson.models.Book;
import irish.franklin.springmssqljson.models.BookJson;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class BookLibraryControllerTest extends MsSqlTestContainerHelper {

    @Autowired
    BookLibraryRepository bookLibraryRepository;

    Book englishBook = Book.builder()
            .name("English Book")
            .description("Test Book Description")
            .language("English")
            .build();

    Book spanishBook = Book.builder()
            .name("Spanish Test Book")
            .description("Spanish Test Book Description")
            .build();

    @Autowired
    private WebTestClient webTestClient;

    public BookJson saveBook(Book book){
        try{
            BookJson bookJson = BookJson.builder()
                    .book(book)
                    .build();
            return bookLibraryRepository.save(bookJson);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup(){
        bookLibraryRepository.deleteAll();
    }

    @Test
    void shouldReturnAllBooksWhenNoQueryParamsArePassedWithA200StatusCode(){
        BookJson savedEnglishBookJson = saveBook(englishBook);
        BookJson savedSpanishBookJson = saveBook(spanishBook);

        webTestClient
            .get()
            .uri("/api/v1/book")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(Book.class)
            .contains(savedEnglishBookJson.getBook())
            .contains(savedSpanishBookJson.getBook());
    }

    @Test
    void shouldCreateNewBookAndReturn201Status() {
        webTestClient.post()
                .uri("/api/v1/book")
                .bodyValue(englishBook)
                .exchange()
                .expectStatus()
                .isCreated();
    }

    @Test
    void shouldReturn400StatusWhenAttemptingToCreateABookWithAnIdInRequestBody() {
        Book bookWithId = Book.builder()
                .id(100)
                .name("Book")
                .description("Book description")
                .build();

        webTestClient.post()
                .uri("/api/v1/book")
                .bodyValue(bookWithId)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldCreateNewBookWithJsonAdditionalPropertiesAndReturn201Status() throws JSONException, JsonProcessingException {
        JSONObject additionalProperties = new JSONObject();
        additionalProperties.put("yearPublished", 2020);
        additionalProperties.put("randomAdditionalProp", "randomValue");
        Book bookWithAdditionalProp = Book.builder()
                .name("Test Book")
                .description("Test Json Book Description")
                .additionalProperties(JacksonUtil.toJsonNode(additionalProperties.toString()))
                .build();


        webTestClient.post()
                .uri("/api/v1/book")
                .bodyValue(bookWithAdditionalProp)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.additionalProperties.randomAdditionalProp")
                    .isEqualTo(additionalProperties.getString("randomAdditionalProp"))
                .jsonPath("$.additionalProperties.yearPublished")
                    .isEqualTo(additionalProperties.getInt("yearPublished"));
    }

    @Test
    void shouldFindAllBooksByDynamicJsonAdditionalPropertiesFieldAndReturn200Status() throws JSONException {
        JSONObject additionalProperties = new JSONObject();
        additionalProperties.put("yearPublished", 2020);
        additionalProperties.put("randomAdditionalProp", "randomValue");
        Book bookWithAdditionalProp = Book.builder()
                .name("Test Book")
                .description("Test Json Book Description")
                .additionalProperties(JacksonUtil.toJsonNode(additionalProperties.toString()))
                .build();


        saveBook(bookWithAdditionalProp);
        saveBook(bookWithAdditionalProp);

        webTestClient.get()
                .uri("/api/v1/book?additionalProperties.yearPublished=" + additionalProperties.getInt("yearPublished"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].additionalProperties.randomAdditionalProp")
                .isEqualTo(additionalProperties.getString("randomAdditionalProp"))
                .jsonPath("$[1].additionalProperties.randomAdditionalProp")
                .isEqualTo(additionalProperties.getString("randomAdditionalProp"));
    }

    @Test
    void shouldFindAllBooksByTwoJsonPropertiesReturnProperBooksAnd200Status() throws JSONException {
        JSONObject additionalPropertiesAnything2020 = new JSONObject();
        additionalPropertiesAnything2020.put("yearPublished", 2020);
        additionalPropertiesAnything2020.put("randomAdditionalProp", "Anything");

        JSONObject additionalPropertiesRandomValue2019 = new JSONObject();
        additionalPropertiesRandomValue2019.put("yearPublished", 2019);
        additionalPropertiesRandomValue2019.put("randomAdditionalProp", "RandomValue");

        Book bookWithAdditionalPropsAnything2020 = Book.builder()
                .name("Test Book")
                .description("Test Json Book Description")
                .additionalProperties(JacksonUtil.toJsonNode(additionalPropertiesAnything2020.toString()))
                .build();
        BookJson savedBookWithAdditionalPropsAnything2020 = saveBook(bookWithAdditionalPropsAnything2020);

        Book bookWithAdditionalPropsRandomValue2019 = Book.builder()
                .name("Test Book")
                .description("Test Json Book Description")
                .additionalProperties(JacksonUtil.toJsonNode(additionalPropertiesRandomValue2019.toString()))
                .build();
        BookJson savedBookWithAdditionalPropsRandomValue2019 = saveBook(bookWithAdditionalPropsRandomValue2019);

        webTestClient.get()
                .uri("/api/v1/book?additionalProperties.yearPublished=2019&name=Test Book")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Book.class)
                .contains(savedBookWithAdditionalPropsRandomValue2019.getBook())
                .doesNotContain(savedBookWithAdditionalPropsAnything2020.getBook());
    }

    @Test
    void shouldThrowBadRequestWhenFindBooksByDynamicJsonWithThreeQueryParams() {
        webTestClient.get()
                .uri("/api/v1/book?additionalProperties.yearPublished=random&additionalProperties.randomAdditionalProp=RandomValue&name=TestBook")
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldGetBookByValidId(){
        BookJson savedEnglishBook = saveBook(englishBook);

        webTestClient.get()
                .uri("/api/v1/book/"+ savedEnglishBook.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Book.class)
                .isEqualTo(savedEnglishBook.getBook());
    }

    @Test
    void shouldReturn404StatusWhenFindBookByIdIsNotInDb(){
        webTestClient.get()
                .uri("/api/v1/book/-100")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void shouldReturnBooksByLanguage(){
        BookJson savedEnglishBookJson = saveBook(englishBook);
        BookJson savedSpanishBookJson = saveBook(spanishBook);

        webTestClient.get()
                .uri("/api/v1/book?language=English")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Book.class)
                .hasSize(1)
                .contains(savedEnglishBookJson.getBook())
                .doesNotContain(savedSpanishBookJson.getBook());
    }

    @Test
    void shouldUpdateBookById() throws JSONException {
        BookJson savedEnglishBook = saveBook(englishBook);

        JSONObject updateBookProperties = new JSONObject();
        updateBookProperties
                .put("description" , "Updated Book Description")
                .put("language", "French");

        webTestClient.patch()
                .uri("/api/v1/book/" + savedEnglishBook.getId())
                .contentType(MediaType.valueOf("application/merge-patch+json"))
                .bodyValue(updateBookProperties.toString())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo(updateBookProperties.get("description"))
                .jsonPath("$.language").isEqualTo(updateBookProperties.get("language"))
                .jsonPath("$.name").isEqualTo(englishBook.getName());
    }

    @Test
    void shouldThrowBadRequestWhenAttemptingToUpdateId() throws JSONException {
        BookJson savedEnglishBook = saveBook(englishBook);

        JSONObject updateBookProperties = new JSONObject();
        updateBookProperties
                .put("id" , "newID");

        webTestClient.patch()
                .uri("/api/v1/book/" + savedEnglishBook.getId())
                .contentType(MediaType.valueOf("application/merge-patch+json"))
                .bodyValue(updateBookProperties.toString())
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    @Test
    void shouldReturn404StatusWhenUpdateBookWithIdNotInDb() throws JSONException {
        JSONObject updateBookProperties = new JSONObject();
        updateBookProperties
                .put("description" , "Updated Book Description")
                .put("language", "French");

        webTestClient.patch()
                .uri("/api/v1/book/-100")
                .contentType(MediaType.valueOf("application/merge-patch+json"))
                .bodyValue(updateBookProperties.toString())
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void deleteBookWithExistingBookIdReturns200() {
        BookJson savedEnglishBook = saveBook(englishBook);

        webTestClient.delete()
                .uri("/api/v1/book/" + savedEnglishBook.getId())
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void deleteBookWithNoBookIdReturns405() {
        webTestClient.delete()
                .uri("/api/v1/book/")
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void deleteBookWithInvalidBookIdReturns404() {
        webTestClient.delete()
                .uri("/api/v1/book/-100")
                .exchange()
                .expectStatus()
                .isNotFound();
    }
}
