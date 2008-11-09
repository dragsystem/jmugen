package org.lee.mugen.background;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lee.mugen.parser.air.AirParser;
import org.lee.mugen.parser.type.StringValueable;
import org.lee.mugen.parser.type.Valueable;
import org.lee.mugen.sprite.base.AbstractAnimManager;
import org.lee.mugen.sprite.baseForParse.SpriteSFF;
import org.lee.mugen.sprite.cns.eval.function.StateCtrlFunction;
import org.lee.mugen.sprite.parser.ExpressionFactory;
import org.lee.mugen.sprite.parser.Parser.GroupText;
import org.lee.mugen.stage.Stage;
import org.lee.mugen.util.BeanTools;
import org.lee.mugen.util.MugenTools;

public class Background {
	private Object root;

	private BGdef bgdef;
	private List<BG> bgs = new ArrayList<BG>();
	private Map<Integer, ArrayList<BG>> bgsMap = new HashMap<Integer, ArrayList<BG>>();

	private Map<Integer, BGCtrlDef> bgCtrlDefMap = new HashMap<Integer, BGCtrlDef>();

	private AbstractAnimManager anim;
	private File currentDir;

	// private String _END = "(?:(?: *;.*$)|(?: *$))";
	private String bgdefRegex = " *bgdef *";
	private String bgRegex = "( *bg +" + "(.*)\\s*)|(bg)";
	private String bgctrldefRegex = " *bgctrldef +" + "(.*) *";
	private String bgCtrlRegex = " *bgctrl +" + "([a-zA-Z0-9\\.\\ \\-\\_]*) *";
	public static final String _GRP_ACTION_REGEX = " *begin action +(\\d*) *";

	public Background(Object parent, File currentDir, String bgdefRegex,
			String bgRegex, String bgctrldefRegex, String bgCtrlRegex) {
		this.bgdefRegex = bgdefRegex;
		this.bgRegex = bgRegex;
		this.bgctrldefRegex = bgctrldefRegex;
		this.bgCtrlRegex = bgCtrlRegex;
		this.currentDir = currentDir;
		root = parent;
	}

	public File getCurrentDir() {
		return currentDir;
	}

	public void parse(Object root, List<GroupText> groups) throws Exception {

		BGCtrlDef parentBGCtrlDef = null;

		Pattern bgdefPattern = Pattern.compile(bgdefRegex,
				Pattern.CASE_INSENSITIVE);
		Pattern bgPattern = Pattern.compile(bgRegex, Pattern.CASE_INSENSITIVE);
		Pattern bgCtrlDefPattern = Pattern.compile(bgctrldefRegex,
				Pattern.CASE_INSENSITIVE);
		Pattern bgCtrlPattern = Pattern.compile(bgCtrlRegex,
				Pattern.CASE_INSENSITIVE);
		Pattern animGrpPattern = Pattern.compile(_GRP_ACTION_REGEX,
				Pattern.CASE_INSENSITIVE);

		AirParser airParser = new AirParser();

		for (GroupText grp : groups) {
			if (bgdefPattern.matcher(grp.getSection()).find()) {
				bgdef = new BGdef();
				for (String key: grp.getKeysOrdered())
					bgdef.parse(this, key, grp.getKeyValues().get(key));
			} else if (animGrpPattern.matcher(grp.getSection()).find()) {
				airParser.parseGroup(grp);
			} else if (bgPattern.matcher(grp.getSection()).find()
					|| "bg".equals(grp.getSection())) {
				String bgName;
				if (!"bg".equalsIgnoreCase(grp.getSection())) {
					Matcher m = bgPattern.matcher(grp.getSection());
					m.find();
					bgName = m.group(1);
				} else {
					bgName = "";
				}
				BG bg = new BG(this);

				bg.setName(bgName);
				bgs.add(bg);
				fillBg("", bg, grp);
				if (bg.getId() != null) {
					ArrayList<BG> list = bgsMap.get(bg.getId());
					if (list == null) {
						list = new ArrayList<BG>();
						bgsMap.put(bg.getId(), list);
					}
					bg.setOrder(list.size());
					list.add(bg);
				}
			} else if (bgCtrlDefPattern.matcher(grp.getSection()).find()) {

				parentBGCtrlDef = BGCtrlDef.parseBGCtrlDef(this, grp
						.getSection(), grp);
				bgCtrlDefMap.put(parentBGCtrlDef.getCtrlid(), parentBGCtrlDef);
			} else if (bgCtrlPattern.matcher(grp.getSection()).find()) {
				BGCtrlDef.BGCtrl.parseBGCtrl(parentBGCtrlDef, parentBGCtrlDef
						.getId(), grp.getSection(), grp);
			}
		}
		anim = new AbstractAnimManager(airParser);
		StateCtrlFunction.endOfParsing();
	}

	private void fillBg(String parent, Object bean, GroupText grp) {
		for (String key : grp.getKeyValues().keySet()) {
			final String value = grp.getKeyValues().get(key);

			Object[] objectValues = null;

			if (value == null || value.trim().length() == 0) {
				objectValues = new Object[] { null };
			} else {
				Valueable[] values = null;
				if ("bgmusic".equalsIgnoreCase(key)
						|| "spr".equalsIgnoreCase(key)
						|| "type".equalsIgnoreCase(key)) {
					values = new Valueable[] { new StringValueable(value) };
				} else {
					String[] tokens = ExpressionFactory
							.expression2Tokens(value);
					values = ExpressionFactory.evalExpression(tokens, false,
							true);
				}
				objectValues = new Object[values.length];
				if ("delta".equalsIgnoreCase(key)) {
					objectValues = new Object[2];
					objectValues[0] = 1;
					objectValues[1] = 1;
				}
				for (int i = 0; i < values.length; ++i) {
					objectValues[i] = values[i].getValue(null);
				}
			}

			try {
				if (objectValues.length == 1) {
					BeanTools.setObject(bean, (parent == null
							|| parent.trim().length() == 0 ? "" : parent + ".")
							+ key, objectValues[0]);
				} else if (objectValues.length > 1) {
					BeanTools.setObject(bean, (parent == null
							|| parent.trim().length() == 0 ? "" : parent + ".")
							+ key, objectValues);

				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(bean.getClass() + " -> " + key + " : "
						+ MugenTools.toString(objectValues));
			}
		}
	}

	// ///////////////////////////////////
	public Object getRoot() {
		return root;
	}

	public BGdef getBgdef() {
		return bgdef;
	}

	public List<BG> getBgs() {
		return bgs;
	}

	public Map<Integer, ArrayList<BG>> getBgsMap() {
		return bgsMap;
	}

	public Map<Integer, BGCtrlDef> getBgCtrlDefMap() {
		return bgCtrlDefMap;
	}

	public AbstractAnimManager getAnim() {
		return anim;
	}

	public String getBgdefRegex() {
		return bgdefRegex;
	}

	public String getBgRegex() {
		return bgRegex;
	}

	public String getBgctrldefRegex() {
		return bgctrldefRegex;
	}

	public String getBgCtrlRegex() {
		return bgCtrlRegex;
	}

	public void process() {
		for (BG bg : getBgs()) {

			if (bg.getId() != null
					&& getBgCtrlDefMap().get(bg.getId()) != null) {
				getBgCtrlDefMap().get(bg.getId()).process();
			} else {
				bg.process();
			}
		}
	}

	public void free() {
		bgdef.free();
		
	}
}
