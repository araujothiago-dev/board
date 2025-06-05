package br.com.dio.ui;

import br.com.dio.persistence.entity.BoardColumnEntity;
import br.com.dio.persistence.entity.BoardColumnKindEnum;
import br.com.dio.persistence.entity.BoardEntity;
import br.com.dio.service.BoardQueryService;
import br.com.dio.service.BoardService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static br.com.dio.persistence.config.ConnectionConfig.getConnection;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.*;

public class MainMenu {
    private final Scanner scanner = new Scanner(System.in).useDelimiter("\n");

    public void execute() throws SQLException {
        while (true) {
            System.out.println("Bem vindo ao gerenciador de boards, escolha uma opção");
            int option = -1;

            System.out.println("1 - Criar um novo board");
            System.out.println("2 - Selecionar um board existente");
            System.out.println("3 - Excluir um board");
            System.out.println("4 - Sair");

            option = readInt();

            switch (option) {
                case 1 -> createBoard();
                case 2 -> selectBoard();
                case 3 -> deleteBoard();
                case 4 -> System.exit(0);
                default -> System.out.println("Opção inválida, informe uma opção do menu.\n");
            }
        }
    }

    private void createBoard() throws SQLException{
        try {
            BoardEntity entity = new BoardEntity();

            System.out.println("Informe o nome do seu board. ");

            entity.setName(readString());

            int additionalColumns = 0;
            do {
                System.out.println("Seu board terá colunas além das 3 padrões? Se sim informe quantas, senão digite '0'");
                additionalColumns = readInt();


                if (additionalColumns < 0) {
                    System.out.println("Opção inválida. Informe um número inteiro.\n");
                    continue;
                }
            } while (additionalColumns < 0);

            List<BoardColumnEntity> columns = new ArrayList<>();

            System.out.println("Informe o nome da coluna inicial do board. ");
            String initialColumnName = readString();
            columns.add(createColumn(initialColumnName, INITIAL, 0));

            for(int i = 0; i < additionalColumns; i++) {
                System.out.println("Informe o nome da coluna de tarefa pendente do board. ");
                String pendingColumnName = scanner.next();
                columns.add(createColumn(pendingColumnName, PENDING, i + 1));
            }

            System.out.println("Informe o nome da coluna final. ");
            String finalColumnName = readString();
            columns.add(createColumn(finalColumnName, FINAL, +additionalColumns + 1));

            System.out.println("Informe o nome da coluna de cancelamento do baord. ");
            String cancelColumnName = readString();
            columns.add(createColumn(cancelColumnName, CANCEL, additionalColumns + 2));

            entity.setBoardColumns(columns);

            try(var connection = getConnection()) {
                BoardService service = new BoardService(connection);
                service.insert(entity);
            }

            System.out.println("Board criado com sucesso.");
        } catch (Exception e) {
            System.out.printf("Não foi possível criar board. Erro: " + e.getMessage() + "\n") ;
        }
    }

    private void selectBoard() throws SQLException{
        System.out.println("Informe o id do board que deseja selecionar. ");
        var id = readLong();
        try(var connection = getConnection()) {
            var service = new BoardQueryService(connection);
            var optional = service.findById(id);
            service.findById(id).ifPresentOrElse(
                    b -> new BoardMenu(b).excute(optional),
                    () -> System.out.printf("Não foi encontrado board com o id %s\n", id)
            );
        }
    }

    private void deleteBoard() throws SQLException{
        System.out.println("Informe o id do boarde que deseja excluir. ");
        var id = readLong();
        try(var connection = getConnection()) {
            var service = new BoardService(connection);
            if (service.delete(id)) {
                System.out.printf("O board %s foi excluído com sucesso\n", id);
            } else {
                System.out.printf("Não foi encontrado um board com id %s\n", id);
            }
        }
    }

    private BoardColumnEntity createColumn(final String name, final BoardColumnKindEnum kind, final int order) {
        var boardColum = new BoardColumnEntity();
        boardColum.setName(name);
        boardColum.setKind(kind);
        boardColum.setOrder(order);
        return boardColum;
    }

    private Long readLong() {
        while (true) {
            String option = scanner.nextLine().trim();
            try {
                return Long.parseLong(option);
            } catch (NumberFormatException e) {
                System.out.print("Opção inválida. Informe um número inteiro:\n");
            }
        }
    }

    private int readInt() {
        while (true) {
            String option = scanner.nextLine().trim();
            try {
                return Integer.parseInt(option);
            } catch (NumberFormatException e) {
                System.out.print("Opção inválida. Informe um número inteiro:\n");
            }
        }
    }

    private String readString() {
        while (true) {
            String option = scanner.nextLine().trim();
            if (!option.isEmpty()) {
                return option;
            }
            System.out.print("Esse campo não pode ser vazio:\n");
        }
    }

}
