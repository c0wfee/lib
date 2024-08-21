package com.example.lm.Service;

import com.example.lm.Dao.BorrowRepository;
import com.example.lm.Dao.FileInfoDao;
import com.example.lm.Model.Borrow;
import com.example.lm.Model.BorrowDTO;
import com.example.lm.Model.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BorrowService {
    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    FileInfoDao fileInfoDao;

    public Borrow saveBorrow(Borrow borrow, int borrowDays) {
        // 获取当前时间
        Date now = new Date();

        // 格式化当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedStartDate = sdf.format(now);

        // 设置借书的开始时间
        borrow.setLoanStartTime(formattedStartDate);

        // 计算结束时间（例如借书期限为 borrowDays 天）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_YEAR, borrowDays);
        String formattedEndDate = sdf.format(calendar.getTime());

        // 设置借书的结束时间
        borrow.setLoanEndTime(formattedEndDate);

        // 保存借书实体
        return borrowRepository.save(borrow);
    }

    public Optional<Borrow> getBorrowByBookId(Integer bookId) {
        return borrowRepository.findByBookId(bookId);
    }

    public Borrow getBorrowByBorrowId(Integer bookId) {
        return borrowRepository.getBorrowByBorrowId(bookId);
    }

    public Borrow saveInfo(Borrow borrow) {
        return borrowRepository.save(borrow);
    }

    public String getLoanEndTime(int bookID) {
        Borrow borrowInfo = borrowRepository.getLoanEndTime(bookID);
        return borrowInfo.getLoanEndTime();
    }

    public String deleteBorrowInfo(int borrowID) {
        borrowRepository.deleteById(borrowID);
        return "Successfully deleted";
    }

    public Page<BorrowDTO> getBorrowInfoForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Borrow> borrows = borrowRepository.findAll(pageable);

        List<BorrowDTO> borrowDTOList = new ArrayList<>();

        for (Borrow borrow : borrows) {
            int bookID = borrow.getBookId();
            String bookTitle = borrow.getBookTitle();
            // 查找book表中的ISBN和database_name
            FileInfo book = fileInfoDao.findById(bookID);
            String isbn = book.getIsbn();
            String databaseName = book.getDatabaseName();

            // 创建BorrowDTO对象并添加到列表中
            BorrowDTO borrowDTO = new BorrowDTO(borrow.getBorrowId(), borrow.getUsername(), borrow.getBookId(), borrow.getLoanStartTime(), borrow.getLoanEndTime(),bookTitle, borrow.getReturnedDate(), borrow.getStatus(), isbn, databaseName);
            borrowDTOList.add(borrowDTO);
        }

        // 将List<BorrowDTO>转换为Page<BorrowDTO>对象
        return new PageImpl<>(borrowDTOList, pageable, borrows.getTotalElements());
    }

    public Map<String, Long> getBorrowCountByUserForCurrentYear() {
        int currentYear = Year.now().getValue();
        List<Object[]> results = borrowRepository.findBorrowCountByUserForCurrentYear(currentYear);

        Map<String, Long> borrowCountByUser = new HashMap<>();
        for (Object[] result : results) {
            String username = (String) result[0];
            Long count = (Long) result[1];
            borrowCountByUser.put(username, count);
        }

        return borrowCountByUser;
    }

    public void updateOverDueBorrows() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = now.format(formatter);


        List<Borrow> overdueBorrows = borrowRepository.getOverDueDate(currentTime);

        for (Borrow borrow : overdueBorrows) {
            borrow.setStatus("Returned");
            borrow.setReturnedDate(currentTime);
            borrowRepository.save(borrow);

            Optional<FileInfo> bookOptional = fileInfoDao.findById(borrow.getBookId());

            if (bookOptional.isPresent()) {
                FileInfo book = bookOptional.get();
                book.setLoanLabel("Returned");
                fileInfoDao.save(book);
            } else {
                System.out.println("Book with ID " + borrow.getBookId() + " not found.");
            }

        }
    }

}
