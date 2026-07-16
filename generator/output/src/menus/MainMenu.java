package menus;

import repository.*;
import util.InputUtils;
import java.util.*;
import java.time.*;

public class MainMenu {

    private Scanner scanner = new Scanner(System.in);
    private LibraryRepository libraryRepository = new LibraryRepository();
    private MemberRepository memberRepository = new MemberRepository();
    private BookRepository bookRepository = new BookRepository();
    private AuthorRepository authorRepository = new AuthorRepository();
    private LoanRepository loanRepository = new LoanRepository();
    private LibraryCardRepository libraryCardRepository = new LibraryCardRepository();
    private StudentMemberRepository studentMemberRepository = new StudentMemberRepository(memberRepository);
    private StaffMemberRepository staffMemberRepository = new StaffMemberRepository(memberRepository);

    public void run() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("1. Library Menu");
            System.out.println("2. StudentMember Menu");
            System.out.println("3. StaffMember Menu");
            System.out.println("4. Book Menu");
            System.out.println("5. Author Menu");
            System.out.println("6. Loan Menu");
            System.out.println("7. LibraryCard Menu");
            System.out.println("0. Exit");

            int option = InputUtils.readInt(scanner, "Option: ");
            switch (option) {
                case 1: new LibraryMenu(scanner, libraryRepository, memberRepository, bookRepository).run(); break;
                case 2: new StudentMemberMenu(scanner, studentMemberRepository, libraryRepository, loanRepository, libraryCardRepository).run(); break;
                case 3: new StaffMemberMenu(scanner, staffMemberRepository, libraryRepository, loanRepository, libraryCardRepository).run(); break;
                case 4: new BookMenu(scanner, bookRepository, loanRepository, authorRepository).run(); break;
                case 5: new AuthorMenu(scanner, authorRepository, bookRepository).run(); break;
                case 6: new LoanMenu(scanner, loanRepository, memberRepository, bookRepository).run(); break;
                case 7: new LibraryCardMenu(scanner, libraryCardRepository, memberRepository).run(); break;
                case 0: System.out.println("Bye!"); return;
                default: System.out.println("Invalid option.");
            }
        }
    }

}
