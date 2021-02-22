package irish.franklin.springmssqljson;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import irish.franklin.springmssqljson.models.Book;
import irish.franklin.springmssqljson.models.BookJson;
import irish.franklin.springmssqljson.utils.JsonMergePatchUtils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/book")
@RequiredArgsConstructor
@Validated
public class BookController {
    private final BookLibraryRepository repository;

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No book found for provided Id"))
                .getBook();
    }

    @GetMapping
    public List<Book> getBooksByQueryParams(@RequestParam Map<String,String> queryParamMap){
        List<BookJson> foundBooks;
        String[] queryParamKeys = queryParamMap.keySet().toArray(new String[0]);
        if (queryParamMap.isEmpty()) {
            foundBooks = repository.findAll();
        }
        else if (queryParamMap.size() == 1) {
            foundBooks = repository.findBooksBySingleParam(
                queryParamKeys[0],
                queryParamMap.get(queryParamKeys[0])
            );
        } else if (queryParamMap.size() == 2) {
            foundBooks = repository.findBooksByTwoParams(
                    queryParamKeys[0],
                    queryParamMap.get(queryParamKeys[0]),
                    queryParamKeys[1],
                    queryParamMap.get(queryParamKeys[1])
            );
        } else {
            // Set to two for this example, but adding an additional else if for 3 or more query params would work
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The max amount of query params that can be used is currently 2");
        }
        return foundBooks.stream()
            .map(BookJson::getBook)
            .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book createNewBook(@Valid @RequestBody Book book) {
        if (book.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Book Ids are auto generated");
        }

        BookJson bookJsonToSave = BookJson.builder()
                .book(book)
                .build();
        return repository.save(bookJsonToSave).getBook();
    }

    @PatchMapping(path = "/{id}", consumes = "application/merge-patch+json")
    public Book patchBook(@PathVariable("id") Integer id,
                         @RequestBody JsonNode bookPropertiesPatchNode){

        if (bookPropertiesPatchNode.has("id")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update Id");
        }

        Optional<BookJson> book = repository.findById(id);

        if(book.isPresent()){
            JSONObject bookProperties = new JSONObject();
            bookProperties.put("book", new JSONObject(bookPropertiesPatchNode.toString()));
            JsonNode bookJsonNode = JacksonUtil.toJsonNode(bookProperties.toString());
            BookJson updatedBook = JsonMergePatchUtils.mergePatch(book.get(), bookJsonNode, BookJson.class);
            return repository.save(updatedBook).getBook();
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No book found for provided Id");
    }

    @DeleteMapping("/{id}")
    public void deleteBookById(@PathVariable Integer id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No book found for provided Id");
    }
}
