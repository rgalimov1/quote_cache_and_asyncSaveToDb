package com.luxoft.quote.dao;

import com.luxoft.quote.domain.Elvl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ElvlRepository {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void ElvlRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Elvl> fetchAllElvls() {
        return this.jdbcTemplate.query( "select isin, elvl from Elvl", new ElvlMapper());
    }

    @Transactional
    public int[] batchUpdateElvls(final List<Elvl> elvls) {
        return this.jdbcTemplate.batchUpdate(
                "merge into Elvl key(isin) values (?, ?)",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Elvl elvl = elvls.get(i);
                        ps.setString(1, elvl.getIsin());
                        ps.setBigDecimal(2, elvl.getElvl());
                    }
                    public int getBatchSize() {
                        return elvls.size();
                    }
                });
    }

    private static final class ElvlMapper implements RowMapper<Elvl> {

        public Elvl mapRow(ResultSet rs, int rowNum) throws SQLException {
            Elvl elvl = new Elvl();
            elvl.setIsin(rs.getString("isin"));
            elvl.setElvl(rs.getBigDecimal("elvl"));
            return elvl;
        }
    }
}


