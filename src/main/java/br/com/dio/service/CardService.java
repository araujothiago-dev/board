package br.com.dio.service;

import br.com.dio.dto.BoardColumnInfoDTO;
import br.com.dio.dto.CardDetailsDTO;
import br.com.dio.exception.CardBlockedException;
import br.com.dio.exception.CardFinishException;
import br.com.dio.exception.EntityNotFoundException;
import br.com.dio.persistence.dao.BlockDAO;
import br.com.dio.persistence.dao.CardDAO;
import br.com.dio.persistence.entity.CardEntity;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static br.com.dio.persistence.entity.BoardColumnKindEnum.CANCEL;
import static br.com.dio.persistence.entity.BoardColumnKindEnum.FINAL;

@AllArgsConstructor
public class CardService {
    private final Connection connection;

    public CardEntity create(final CardEntity entity) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            dao.insert(entity);
            connection.commit();
            return entity;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void moveToNextColumn(final Long cardId, final List<BoardColumnInfoDTO> boardColumnInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var dto = getCard(dao, cardId);

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId));
            }

            var currentColumn = getCurrentColumn(dto.columnId(), boardColumnInfo);

            if (currentColumn.kind().equals(FINAL)) {
                throw new CardFinishException("O card já foi finalizado");
            }

            var nextColumn = getNextColumn(currentColumn, boardColumnInfo);

            dao.moveToColumn(nextColumn.id(), cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void cancel(final Long cardId, final Long cancelColumnId, final List<BoardColumnInfoDTO> boardColumnInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var dto = getCard(dao, cardId);

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId));
            }

            var currentColumn = getCurrentColumn(dto.columnId(), boardColumnInfo);

            if (currentColumn.kind().equals(FINAL)) {
                throw new CardFinishException("O card já foi finalizado");
            }

            getNextColumn(currentColumn, boardColumnInfo);

            dao.moveToColumn(cancelColumnId, cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void block(final Long cardId, final String reason, final List<BoardColumnInfoDTO> boardColumnInfo) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var dto = getCard(dao, cardId);

            if (dto.blocked()) {
                throw new CardBlockedException("O card %s está bloqueado, é necesário desbloquea-lo para mover".formatted(cardId));
            }
            var currentColumn = getCurrentColumn(dto.columnId(), boardColumnInfo);

            if (currentColumn.kind().equals(FINAL) || currentColumn.kind().equals(CANCEL)) {
                throw new IllegalStateException("O card não pode ser bloqueado porque está na coluna do tipo %s".formatted(currentColumn.kind()));
            }

            var blockDAO = new BlockDAO(connection);
            blockDAO.block(reason, cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    public void unblock(final Long cardId, final String reason) throws SQLException {
        try{
            var dao = new CardDAO(connection);
            var dto = getCard(dao, cardId);

            if(!dto.blocked()) {
                throw new CardBlockedException("O card %s não está bloqueado".formatted(cardId));
            }

            var blockDAO = new BlockDAO(connection);
            blockDAO.unblock(reason, cardId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }


    private BoardColumnInfoDTO getCurrentColumn(Long columnId, List<BoardColumnInfoDTO> boardColumnInfo) {
        return boardColumnInfo.stream()
                .filter(bc -> bc.id().equals(columnId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("O card informado pertence a outro board"));
    }

    private BoardColumnInfoDTO getNextColumn(BoardColumnInfoDTO current, List<BoardColumnInfoDTO> boardColumnInfo) {
        return boardColumnInfo.stream()
                .filter(bc -> bc.order() == current.order() + 1)
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("O card está cancelado.")
                );
    }

    private CardDetailsDTO getCard(CardDAO dao, Long cardId) throws SQLException {
        return dao.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "O card de id %s não foi encontrado.".formatted(cardId)
                ));
    }
}
