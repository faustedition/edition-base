package de.faustedition.document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.google.common.collect.Lists;

public class HandProperties implements Comparable<HandProperties> {
	private Integer id;
	private final Scribe scribe;
	private final WritingMaterial material;
	private final FontStyle style;

	public HandProperties(Scribe scribe, WritingMaterial material, FontStyle style) {
		this.scribe = scribe;
		this.material = material;
		this.style = style;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Scribe getScribe() {
		return scribe;
	}

	public WritingMaterial getMaterial() {
		return material;
	}

	public FontStyle getStyle() {
		return style;
	}

	public String getKey() {
		List<String> keys = Lists.newArrayList();

		keys.add(scribe == null ? "" : scribe.getKey());

		if (material != null) {
			keys.add(material.getKey());
		}

		if (style != null && style.getKey() != null) {
			keys.add(style.getKey());
		}

		return StringUtils.join(keys, "_");
	}

	public String getDescription() {
		StringBuilder description = new StringBuilder(scribe.getFullname());
		if (material != null || style != null) {
			description.append(" (");

			if (material != null) {
				description.append(material.getDescription());
			}
			if (style != null) {
				description.append(material == null ? "" : " - ");
				description.append(style.getDescription());
			}

			description.append(")");
		}
		return description.toString();
	}

	public MapSqlParameterSource toSqlParameterSource() {
		MapSqlParameterSource ps = new MapSqlParameterSource();
		ps.addValue("scribe", scribe == null ? null : scribe.toString());
		ps.addValue("material", material == null ? null : material.toString());
		ps.addValue("style", style == null ? null : style.toString());
		return ps;
	}

	public static final RowMapper<HandProperties> ROW_MAPPER = new ParameterizedRowMapper<HandProperties>() {

		@Override
		public HandProperties mapRow(ResultSet rs, int rowNum) throws SQLException {
			final String scribe = rs.getString("scribe");
			final String material = rs.getString("material");
			final String style = rs.getString("style");
			HandProperties hp = new HandProperties(//
					scribe == null ? null : Scribe.valueOf(scribe),//
					material == null ? null : WritingMaterial.valueOf(material),//
					style == null ? null : FontStyle.valueOf(style));

			hp.setId(rs.getInt("id"));
			return hp;
		}
	};

	@Override
	public int compareTo(HandProperties o) {
		return id.compareTo(o.id);
	}
}
