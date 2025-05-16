import javax.persistence.*;
import lombok.Data;

@Entity
@Table(name = "books")
@Data  
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String pinyinInitials;
    private String isbn;
    private String author;
    private String category;
}
