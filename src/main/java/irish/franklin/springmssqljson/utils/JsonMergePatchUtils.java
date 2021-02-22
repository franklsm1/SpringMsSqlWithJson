package irish.franklin.springmssqljson.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
public class JsonMergePatchUtils {

    private JsonMergePatchUtils(){
        //private constructor to prevent public initialization
    }

    public static <T> T mergePatch(T currentDbObject, JsonNode patchNode, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.convertValue(currentDbObject, JsonNode.class);
            JsonMergePatch mergePatch = JsonMergePatch.fromJson(patchNode);
            node = mergePatch.apply(node);
            return mapper.treeToValue(node, clazz);
        }
        catch (IOException | JsonPatchException e) {
            log.info("Invalid patch request body: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid patch request body");
        }
    }
}
