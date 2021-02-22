package irish.franklin.springmssqljson;

import irish.franklin.springmssqljson.models.BookJson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookLibraryRepository extends JpaRepository<BookJson, Integer> {
    @Query(
            value = "SELECT *"
                    + " FROM BOOK_LIBRARY"
                    + " WHERE JSON_VALUE(book, :#{'$.' + #fieldName}) = :fieldValue",
            nativeQuery = true
    )
    List<BookJson> findBooksBySingleParam(
            @Param("fieldName") String fieldName,
            @Param("fieldValue") String fieldValue
    );

    @Query(
            value = "SELECT *"
                    + " FROM BOOK_LIBRARY"
                    + " WHERE JSON_VALUE(book, :#{'$.' + #fieldName1}) = :fieldValue1"
                    + " AND JSON_VALUE(book, :#{'$.' + #fieldName2}) = :fieldValue2",
            nativeQuery = true
    )
    List<BookJson> findBooksByTwoParams(
            @Param("fieldName1") String fieldName,
            @Param("fieldValue1") String fieldValue,
            @Param("fieldName2") String fieldName2,
            @Param("fieldValue2") String fieldValue2
    );
}
