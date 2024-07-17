package com.example.lm.Service;


import com.example.lm.Dao.FavourDao;
import com.example.lm.Dao.FileInfoDao;
import com.example.lm.Dao.UserInfoRepository;
import com.example.lm.Dao.UserRepository;
import com.example.lm.Model.Favour;
import com.example.lm.Model.FileInfo;
import com.example.lm.Model.User;
import com.example.lm.Model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserInfoRepository userRepository;

    @Autowired
    private FileInfoDao bookRepository;

    @Autowired
    private FavourDao userBookRepository;

    @Autowired
    private UserRepository userRepositoryForLogin;

    public User authenticate(String username) {
        try {
            User user = userRepositoryForLogin.findByUsername(username);

            if (user == null) {
                System.out.println("User not found: " + username);
                throw new RuntimeException("User not found: " + username);
            }

            System.out.println("User unban time: " + user.getUnbanTime());

            if (user.getUnbanTime() != null && user.getUnbanTime().after(new Timestamp(System.currentTimeMillis()))) {
                System.out.println("User is banned until " + user.getUnbanTime());
                throw new RuntimeException("User is banned until " + user.getUnbanTime());
            }

            return user;
        } catch (NullPointerException e) {
            System.out.println("NullPointerException caught: " + e.getMessage());
            throw new RuntimeException("An error occurred while authenticating the user.");
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
            throw new RuntimeException("An unexpected error occurred while authenticating the user.");
        }
    }


    public void addBookToUserCollection(String username, String bookisbn) {
        UserInfo user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("User not found!");
        }

        FileInfo book = bookRepository.findByisbn(bookisbn);

        if (book == null) {
            throw new IllegalArgumentException("Book not found!");
        }

    // Check if the user already has this book in their collection
        boolean alreadyFavorited = userBookRepository.existsByUserAndBook(user, book);

        if (alreadyFavorited) {
            throw new IllegalArgumentException("Book already in collection!");
        }

        Favour userBook = new Favour();
        userBook.setUser(user);
        userBook.setBook(book);
        userBookRepository.save(userBook);
    }

    public Set<FileInfo> listCollect(String username) {
        UserInfo user = userRepository.findByUsername(username);
        Set<Favour> favours = userBookRepository.findByUser(user);
        Set<FileInfo> books = new HashSet<>();
        for(Favour i :favours){
            books.add(i.getBook());
        }
        return books;
    }

    public void removeBookCollection(String username, String bookisbn) {
        UserInfo user = userRepository.findByUsername(username);
        if (user == null) {

            throw new IllegalArgumentException("User not found!");
        }
        FileInfo book = bookRepository.findByisbn(bookisbn);
        if (book == null) {
            throw new IllegalArgumentException("Book not found!");
        }

        boolean alreadyFavorited = userBookRepository.existsByUserAndBook(user, book);

        if (!alreadyFavorited) {
            throw new IllegalArgumentException("Book not in collection!");
        }

        Optional<Favour> favour = userBookRepository.findByUserAndBook(user, book);
        if (favour.isPresent()) {
            userBookRepository.delete(favour.get());
        }else {throw new IllegalArgumentException("Book not in collection!");}
    }

    public UserInfo userLogin(String username) {
        return userRepository.findByUsername(username);
    }

    public Page<User> findAll(Pageable pageable) {
        System.out.println("Querying database with pageable: " + pageable);
        Page<User> result = userRepositoryForLogin.findAll(pageable);
        System.out.println("Query result: " + result.getContent());
        return result;
    }
}
