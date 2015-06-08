/*******************************************************************************
 * This file is part of ISPyB.
 * 
 * ISPyB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ISPyB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ISPyB.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors : S. Delageniere, R. Leal, L. Launer, K. Levik, S. Veyrier, P. Brenchereau, M. Bodin, A. De Maria Antolinos
 ******************************************************************************************************************************/

package ispyb.client.common.shipping;

import ispyb.client.common.util.DBTools;
import ispyb.client.common.util.ISPyBParser;
import ispyb.common.util.Constants;
import ispyb.common.util.StringUtils;
import ispyb.server.common.services.proposals.Proposal3Service;
import ispyb.server.common.services.shipping.Container3Service;
import ispyb.server.common.services.shipping.Dewar3Service;
import ispyb.server.common.services.shipping.Shipping3Service;
import ispyb.server.common.util.ejb.Ejb3ServiceLocator;
import ispyb.server.common.vos.proposals.Proposal3VO;
import ispyb.server.common.vos.shipping.Container3VO;
import ispyb.server.common.vos.shipping.Dewar3VO;
import ispyb.server.common.vos.shipping.Shipping3VO;
import ispyb.server.mx.services.sample.BLSample3Service;
import ispyb.server.mx.services.sample.Crystal3Service;
import ispyb.server.mx.services.sample.DataMatrixInSampleChanger3Service;
import ispyb.server.mx.services.sample.DiffractionPlan3Service;
import ispyb.server.mx.services.sample.Protein3Service;
import ispyb.server.mx.vos.sample.BLSample3VO;
import ispyb.server.mx.vos.sample.Crystal3VO;
import ispyb.server.mx.vos.sample.DataMatrixInSampleChanger3VO;
import ispyb.server.mx.vos.sample.DiffractionPlan3VO;
import ispyb.server.mx.vos.sample.Protein3VO;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class UploadShipmentUtils {

	private static final Ejb3ServiceLocator ejb3ServiceLocator = Ejb3ServiceLocator.getInstance();

	// Define the positions of all the information in this spreadsheet
	// Note: rows and columns start at zero!
	// private static final int checkRow = 0;

	// private static final short checkCol = 0;

	private static final int puckRow = 1;

	private static final short puckCol = 3;

	private static final int dewarRow = 2;

	private static final short dewarCol = puckCol;

	// private static final String ShippingLabel = "Shipping Id";
	//
	// private static final String ProposalAndShippingLabel = "Proposal Id / Shipping Id";
	//
	// private static final int idLabelRow = 3;
	//
	// private static final short idLabelCol = puckCol - 1;
	//
	// private static final int value1IdRow = 3;
	//
	// private static final short value1IdCol = puckCol;
	//
	// private static final int value2IdRow = 3;
	//
	// private static final short value2IdCol = puckCol + 1;

	private static final int dataRow = 6;

	private static final short samplePosCol = 0;

	private static final short proteinNameCol = 1;

	private static final short proteinAcronymCol = 2;

	private static final short spaceGroupCol = 3;

	private static final short sampleNameCol = 4;

	private static final short pinBarCodeCol = 5;

	private static final short preObsResolutionCol = 6;

	private static final short neededResolutionCol = 7;

	private static final short oscillationRangeCol = 8;

	private static final short experimentTypeCol = 9;

	private static final short anomalousScattererCol = 10;

	private static final short unitCellACol = 11;

	private static final short unitCellBCol = 12;

	private static final short unitCellCCol = 13;

	private static final short unitCellAlphaCol = 14;

	private static final short unitCellBetaCol = 15;

	private static final short unitCellGammaCol = 16;

	private static final short loopTypeCol = 17;

	private static final short holderLengthCol = 18;

	private static final short commentsCol = 19;

	private static final short courrierNameRow = 1;

	private static final short courrierNameCol = 10;

	private static final short trackingNumberRow = 2;

	private static final short trackingNumberCol = 10;

	private static final short shippingDateRow = 3;

	private static final short shippingDateCol = 10;

	// private static final short proteinAcronymRow = 49; // Log4J
	//
	// private final boolean populateDMCodes = false;

	/**
	 * PopulateTemplate
	 * 
	 * @param request
	 * @param getTemplateFullPathOnly
	 * @param getTemplateFilenameOnly
	 * @param populateDMCodes
	 * @param selectedBeamlineName
	 * @param hashDMCodesForBeamline
	 * @param populateForExport
	 * @param nbContainersToExport
	 * @param populateForShipment
	 * @param shippingId
	 * @return
	 */
	public static String PopulateTemplate(HttpServletRequest request, boolean getTemplateFullPathOnly,
			boolean getTemplateFilenameOnly, boolean populateDMCodes, String selectedBeamlineName, List hashDMCodesForBeamline,
			boolean populateForExport, int nbContainersToExport, boolean populateForShipment, int shippingId) {

		String populatedTemplatePath = "";
		try {
			DataMatrixInSampleChanger3Service dataMatrixInSampleChanger3Service = (DataMatrixInSampleChanger3Service) ejb3ServiceLocator
					.getLocalService(DataMatrixInSampleChanger3Service.class);

			String xlsPath;
			String proposalCode;
			String proposalNumber;
			String populatedTemplateFileName;
			// GregorianCalendar calendar = new GregorianCalendar();
			String today = ".xls";
			if (request != null) {
				xlsPath = Constants.TEMPLATE_POPULATED_RELATIVE_PATH;
				if (populateForShipment)
					xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FROM_SHIPMENT;
				else if (populateForExport) {
					switch (nbContainersToExport) {
					case 1:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME1;
						break;
					case 2:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME2;
						break;
					case 3:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME3;
						break;
					case 4:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME4;
						break;
					case 5:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME5;
						break;
					case 6:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME6;
						break;
					case 7:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME7;
						break;
					case 8:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME8;
						break;
					case 9:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME9;
						break;
					case 10:
						xlsPath = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + Constants.TEMPLATE_XLS_POPULATED_FOR_EXPORT_FILENAME10;
						break;
					}
				}

				proposalCode = (String) request.getSession().getAttribute(Constants.PROPOSAL_CODE);
				proposalNumber = String.valueOf(request.getSession().getAttribute(Constants.PROPOSAL_NUMBER));

				if (populateForShipment)
					populatedTemplateFileName = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber
							+ "_shipment_" + shippingId + today;
				else
					populatedTemplateFileName = Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber
							+ ((populateDMCodes) ? "_#" : "") + today;

				populatedTemplatePath = request.getContextPath() + populatedTemplateFileName;

				if (getTemplateFilenameOnly && populateForShipment)
					return proposalCode + proposalNumber + "_shipment_" + shippingId + today;
				if (getTemplateFilenameOnly && !populateForShipment)
					return proposalCode + proposalNumber + ((populateDMCodes) ? "_#" : "") + today;

				xlsPath = request.getRealPath(xlsPath);
				String prefix = new File(xlsPath).getParent();
				populatedTemplateFileName	= (prefix + "/" + new File(populatedTemplateFileName).getName());
			
			} else {
				xlsPath = "C:/" + Constants.TEMPLATE_POPULATED_RELATIVE_PATH;
				proposalCode = "ehtpx";
				proposalNumber = "1";
				populatedTemplateFileName = "C:/" + Constants.TEMPLATE_RELATIVE_DIRECTORY_PATH + proposalCode + proposalNumber + today;
			}
			if (getTemplateFullPathOnly)
				return populatedTemplateFileName;

			// Retrieve DM codes from DB
			String beamlineName = selectedBeamlineName;

			String[][] dmCodesinSC = null;

			if (populateDMCodes) {
				dmCodesinSC = new String[Constants.SC_BASKET_CAPACITY + 1][Constants.BASKET_SAMPLE_CAPACITY + 1];
				Proposal3VO prop = DBTools.getProposal(proposalCode, proposalNumber);
				if (prop != null) {
					Integer proposalId = prop.getProposalId();
					List lstDMCodes = dataMatrixInSampleChanger3Service.findByProposalIdAndBeamlineName(proposalId, beamlineName);
					for (int i = 0; i < lstDMCodes.size(); i++) {
						DataMatrixInSampleChanger3VO dmInSC = (DataMatrixInSampleChanger3VO) lstDMCodes.get(i);
						Integer basketLocation = dmInSC.getContainerLocationInSC();
						Integer sampleLocation = dmInSC.getLocationInContainer();
						String dmCode = dmInSC.getDatamatrixCode();
						if (basketLocation <= Constants.SC_BASKET_CAPACITY && sampleLocation <= Constants.BASKET_SAMPLE_CAPACITY) {
							dmCodesinSC[basketLocation][sampleLocation] = dmCode;
						}
					}
				}
			}

			File originalTemplate = new File(xlsPath);
			File populatedTemplate = new File(populatedTemplateFileName);
			FileUtils.copyFile(originalTemplate, populatedTemplate);
			ISPyBParser parser = new ISPyBParser();

			// Copy template to tmp folder
			File xlsTemplate = new File(xlsPath);
			File xlsPopulatedTemplate = new File(populatedTemplateFileName);
			FileUtils.copyFile(xlsTemplate, xlsPopulatedTemplate);

			// Get the list of Proteins
			Proposal3Service proposalService = (Proposal3Service) Ejb3ServiceLocator.getInstance().getLocalService(
					Proposal3Service.class);
			List<Proposal3VO> proposals = proposalService.findByCodeAndNumber(proposalCode, proposalNumber, false, true, false);
			Proposal3VO proposalLight = proposals.get(0);

			// List<Protein3VO> listProteins = new ArrayList<Protein3VO>(proposalLight.getProteinVOs());
			Protein3Service protein3Service = (Protein3Service) ejb3ServiceLocator.getLocalService(Protein3Service.class);
			List<Protein3VO> listProteins = protein3Service.findByProposalId(proposalLight.getProposalId(), true, true);

			parser.populate(xlsPath, populatedTemplateFileName, listProteins, dmCodesinSC);

			if (populateForShipment)
				parser.populateExistingShipment(populatedTemplateFileName, populatedTemplateFileName, shippingId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return populatedTemplatePath;
	}

	public static String[] importFromXls(InputStream file, Integer shippingId, boolean deleteAllShipment,
			List<String> allowedSpaceGroups) throws Exception {
		String msgError = "";
		String msgWarning = "";
		Protein3Service proteinService = (Protein3Service) ejb3ServiceLocator.getLocalService(Protein3Service.class);
		Crystal3Service crystalService = (Crystal3Service) ejb3ServiceLocator.getLocalService(Crystal3Service.class);

		BLSample3Service sampleService = (BLSample3Service) ejb3ServiceLocator.getLocalService(BLSample3Service.class);

		DiffractionPlan3Service difPlanService = (DiffractionPlan3Service) ejb3ServiceLocator
				.getLocalService(DiffractionPlan3Service.class);

		Container3Service containerService = (Container3Service) ejb3ServiceLocator.getLocalService(Container3Service.class);
		Shipping3Service shippingService = (Shipping3Service) ejb3ServiceLocator.getLocalService(Shipping3Service.class);

		Shipping3VO shipment = shippingService.findByPk(shippingId, true);
		Set<Dewar3VO> dewars = shipment.getDewarVOs();

		HSSFWorkbook workbook = null;
		Integer sheetProposalId = DBTools.getProposalIdFromShipping(shippingId);

		String courrierName = "";
		String shippingDate = "";
		String trackingNumber = "";

		POIFSFileSystem fs = new POIFSFileSystem(file);
		// Now extract the workbook
		workbook = new HSSFWorkbook(fs);

		// Working through each of the worksheets in the spreadsheet
		// ASSUMPTION: one excel file = one shipment
		for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
			boolean sheetIsEmpty = true;
			HSSFSheet sheet = workbook.getSheetAt(sheetNum);

			// DeliveryAgent ----
			// --- Retrieve Shipment related information
			if (sheetNum == 0) {
				if (sheet.getRow(courrierNameRow).getCell(courrierNameCol) == null) {
					msgError += "The format of the xls file is incorrect (courrier name missing)";
					String[] msg = new String[2];
					msg[0] = msgError;
					msg[1] = msgWarning;
					return msg;
				}
				courrierName = (sheet.getRow(courrierNameRow).getCell(courrierNameCol)).toString();
				if (sheet.getRow(shippingDateRow).getCell(shippingDateCol) == null) {
					msgError += "The format of the xls file is incorrect (shipping Date missing)";
					String[] msg = new String[2];
					msg[0] = msgError;
					msg[1] = msgWarning;
					return msg;
				}
				shippingDate = (sheet.getRow(shippingDateRow).getCell(shippingDateCol)).toString();
				if (sheet.getRow(trackingNumberRow).getCell(trackingNumberCol) == null) {
					msgError += "The format of the xls file is incorrect (tracking number missing)";
					String[] msg = new String[2];
					msg[0] = msgError;
					msg[1] = msgWarning;
					return msg;
				}
				trackingNumber = (sheet.getRow(trackingNumberRow).getCell(trackingNumberCol)).toString();

				// retrieveShippingId(file);

				DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
				Date shipDate = null;
				Calendar shipCal = Calendar.getInstance();
				try {
					shipDate = fmt.parse(shippingDate);
					shipCal.setTime(shipDate);
				} catch (Exception e) {
					shipCal = Calendar.getInstance();
				}
				shipment.setDeliveryAgentAgentCode(trackingNumber);
				shipment.setDeliveryAgentAgentName(courrierName);
				shipment.setDeliveryAgentShippingDate(shipDate);
			}
			// Dewar
			String dewarCode = (sheet.getRow(dewarRow).getCell(dewarCol)).toString().trim();
			Integer dewarId = null;
			Dewar3VO dewar = null;
			// check if dewar exists
			for (Dewar3VO dewar3vo : dewars) {
				if (dewar3vo.getCode().equals(dewarCode)) {
					dewarId = dewar3vo.getDewarId();
					dewar = dewar3vo;
					break;
				}
			}

			Container3VO container = null;
			if (dewar == null) {
				msgError += "Dewar with code '" + dewarCode + "' does not correspond to any dewar. Please check the dewar's name.\n";
				sheetIsEmpty = true;
			} else {
				// Puck
				container = new Container3VO();
				container.setDewarVO(dewar);
				container.setContainerType("Puck");
				container.setCode((sheet.getRow(puckRow).getCell(puckCol)).toString().trim());
				container.setCapacity(Constants.BASKET_SAMPLE_CAPACITY);
				container.setTimeStamp(StringUtils.getCurrentTimeStamp());
				if (!deleteAllShipment) {
					// check sheet empty before
					boolean sheetEmpty = true;
					for (int i = dataRow; i < dataRow + Constants.BASKET_SAMPLE_CAPACITY; i += 1) {
						boolean sampleRowOK = true;
						String puckCode = cellToString(sheet.getRow(puckRow).getCell(puckCol));
						String proteinAcronym = cellToString(sheet.getRow(i).getCell(proteinAcronymCol));
						String sampleName = cellToString(sheet.getRow(i).getCell(sampleNameCol));
						boolean sampleNameRulesOk = sampleName.matches(Constants.MASK_BASIC_CHARACTERS_WITH_DASH_UNDERSCORE_NO_SPACE);

						if (puckCode.isEmpty() || dewarCode.isEmpty() || proteinAcronym.isEmpty() || sampleName.isEmpty()
								|| !sampleNameRulesOk) {
							sampleRowOK = false;
						}
						if (!sampleRowOK) {
							// Skip this line we do not create the sample
						} else {
							sheetEmpty = false;
							break;
						}
					}
					List<Container3VO> listContainerFromDB = containerService.findByBarCode(dewar.getDewarId(), container.getCode());
					if (listContainerFromDB != null && listContainerFromDB.size() > 0 && !sheetEmpty) { // delete it in
																										// order to be
																										// replaced by
																										// the new one
						containerService.deleteByPk(listContainerFromDB.get(0).getContainerId());
						msgWarning += "The Puck " + container.getCode() + " has been deleted and a new one has been added.";
					}
				}
				container = containerService.create(container);
				// List<Crystal3VO> listCrystalCreated = new ArrayList<Crystal3VO>();
				List<Crystal3VO> listCrystalCreated = crystalService.findByProposalId(sheetProposalId);
				// TBD: need to add sanity check that this puck has not already been put in the dewar!

				for (int i = dataRow; i < dataRow + Constants.BASKET_SAMPLE_CAPACITY; i += 1) {
					// --- Retrieve interesting values from spreadsheet
					String puckCode = cellToString(sheet.getRow(puckRow).getCell(puckCol));
					String proteinName = cellToString(sheet.getRow(i).getCell(proteinNameCol));
					String proteinAcronym = cellToString(sheet.getRow(i).getCell(proteinAcronymCol));
					String samplePos = cellToString(sheet.getRow(i).getCell(samplePosCol));
					String sampleName = cellToString(sheet.getRow(i).getCell(sampleNameCol));
					String pinBarCode = cellToString(sheet.getRow(i).getCell(pinBarCodeCol));
					double preObsResolution = cellToDouble(sheet.getRow(i).getCell(preObsResolutionCol));
					double neededResolution = cellToDouble(sheet.getRow(i).getCell(neededResolutionCol));
					double oscillationRange = cellToDouble(sheet.getRow(i).getCell(oscillationRangeCol));
					String experimentType = cellToString(sheet.getRow(i).getCell(experimentTypeCol));
					String anomalousScatterer = cellToString(sheet.getRow(i).getCell(anomalousScattererCol));
					String spaceGroup = cellToString(sheet.getRow(i).getCell(spaceGroupCol)).toUpperCase().trim().replace(" ", "");
					double unitCellA = cellToDouble(sheet.getRow(i).getCell(unitCellACol));
					double unitCellB = cellToDouble(sheet.getRow(i).getCell(unitCellBCol));
					double unitCellC = cellToDouble(sheet.getRow(i).getCell(unitCellCCol));
					double unitCellAlpha = cellToDouble(sheet.getRow(i).getCell(unitCellAlphaCol));
					double unitCellBeta = cellToDouble(sheet.getRow(i).getCell(unitCellBetaCol));
					double unitCellGamma = cellToDouble(sheet.getRow(i).getCell(unitCellGammaCol));
					String loopType = cellToString(sheet.getRow(i).getCell(loopTypeCol));
					double holderLength = cellToDouble(sheet.getRow(i).getCell(holderLengthCol));
					String sampleComments = cellToString(sheet.getRow(i).getCell(commentsCol));

					// Fill in values by default
					// Protein Name
					if (proteinName.equalsIgnoreCase(""))
						proteinName = proteinAcronym;

					// --- Check the Sheet is not empty for this line and all required fields are present ---
					boolean sampleRowOK = true;
					boolean sampleNameRulesOk = sampleName.matches(Constants.MASK_BASIC_CHARACTERS_WITH_DASH_UNDERSCORE_NO_SPACE);
					if (puckCode.isEmpty() || dewarCode.isEmpty() || proteinAcronym.isEmpty() || sampleName.isEmpty()
							|| sampleName.length() > Constants.BLSAMPLE_NAME_NB_CHAR || !sampleNameRulesOk) {
						sampleRowOK = false;
						if (!(sampleName.isEmpty() && proteinAcronym.isEmpty())) {
							msgError += "Error with the sample: " + sampleName;
							if (puckCode.isEmpty()) {
								msgError += " (The puck code is empty)";
							}
							if (dewarCode.isEmpty()) {
								msgError += " (The dewar code is empty)";
							}
							if (proteinAcronym.isEmpty()) {
								msgError += " (The protein acronym is empty)";
							}
							if (sampleName.isEmpty()) {
								msgError += " (The sample name is empty)";
							}
							if (sampleName.length() > Constants.BLSAMPLE_NAME_NB_CHAR) {
								msgError += " (The sample name is too long : max 8 characters)";
							}
							if (!sampleNameRulesOk) {
								msgError += " (The sample name is not well formatted)";
							}
							msgError += "\n.";
						}
					}

					if (!sampleRowOK) {
						// Skip this line we do not create the sample
					} else {
						sheetIsEmpty = false;

						String crystalID = UUID.randomUUID().toString();
						// String diffractionPlanUUID = uuidGenerator.generateRandomBasedUUID().toString();
						if ((null != crystalID) && (!crystalID.equals(""))) {
							// Parse ProteinAcronym - SpaceGroup
							// Pre-filled spreadsheet (including protein_acronym - SpaceGroup)
							int separatorStart = proteinAcronym.indexOf(Constants.PROTEIN_ACRONYM_SPACE_GROUP_SEPARATOR);
							if (separatorStart != -1) {
								String acronym = proteinAcronym.substring(0, separatorStart);
								String prefilledSpaceGroup = proteinAcronym.substring(separatorStart
										+ Constants.PROTEIN_ACRONYM_SPACE_GROUP_SEPARATOR.length(), proteinAcronym.length());
								proteinAcronym = acronym;
								if (allowedSpaceGroups.contains(spaceGroup.toUpperCase())) {
									// Do nothing = use spaceGroup from dropdown list
								} else if (allowedSpaceGroups.contains(prefilledSpaceGroup.toUpperCase())) {
									// Used pre-filled space group
									spaceGroup = prefilledSpaceGroup;
								}
							}
							// Protein
							// We might eventually want to include more details in the spreadsheet, but for the time
							// being
							// just the name is sufficient.
							List<Protein3VO> proteinTab = proteinService.findByAcronymAndProposalId(sheetProposalId, proteinAcronym);
							if (proteinTab == null || proteinTab.size() == 0) {
								msgError += "Protein '" + proteinAcronym + "' can't be found \n ";
							} else {
								Protein3VO protein = proteinTab.get(0);
								// unique sample name
								List<BLSample3VO> samplesWithSameName = sampleService.findByNameAndProteinId(sampleName,
										protein.getProteinId(), shippingId);

								boolean validName = true;
								if (!samplesWithSameName.isEmpty()) {
									validName = false;
									msgError += "[" + protein.getAcronym() + " + " + sampleName
											+ "] is already existing, and should be unique.\n";
								}
								if (validName) {
									// Diffraction Plan
									DiffractionPlan3VO difPlan = new DiffractionPlan3VO();

									difPlan.setAnomalousScatterer(anomalousScatterer);
									difPlan.setObservedResolution(preObsResolution);
									difPlan.setRequiredResolution(neededResolution);
									difPlan.setExposureTime((double) 0);
									difPlan.setOscillationRange(oscillationRange);
									if (experimentType == null || experimentType.isEmpty()) {
										experimentType = "Default";
									}
									difPlan.setExperimentKind(experimentType);

									difPlan = difPlanService.create(difPlan);

									// Crystal
									Crystal3VO crystal = new Crystal3VO();
									crystal.setProteinVO(protein);
									crystal.setDiffractionPlanVO(difPlan);
									crystal.setCrystalUUID(crystalID);
									crystal.setSpaceGroup(spaceGroup);
									if ((crystal.getSpaceGroup() == null) || (crystal.getSpaceGroup().equals(""))) {
										crystal.setSpaceGroup("Undefined");
									} else {

										// TODO SD in the case where space group is not empty and no cell dimensions
										// have been
										// entered,
										// fill the crystal with the default value of the crystal = protein + space
										// group
										List<Crystal3VO> tab = crystalService.findFiltered(sheetProposalId, null, proteinAcronym,
												spaceGroup);
										if (tab != null && tab.size() > 0) {
											Crystal3VO newCrystal3VO = new Crystal3VO();
											int j = 0;
											for (Crystal3VO crystal3vo : tab) {
												newCrystal3VO = tab.get(j);
												j = j + 1;
											}

											if (newCrystal3VO != null && unitCellA == 0 && unitCellB == 0 && unitCellC == 0
													&& unitCellAlpha == 0 && unitCellBeta == 0 && unitCellGamma == 0) {
												unitCellA = (newCrystal3VO.getCellA() == null ? 0 : newCrystal3VO.getCellA());
												unitCellB = (newCrystal3VO.getCellB() == null ? 0 : newCrystal3VO.getCellB());
												unitCellC = (newCrystal3VO.getCellC() == null ? 0 : newCrystal3VO.getCellC());
												unitCellAlpha = (newCrystal3VO.getCellAlpha() == null ? 0 : newCrystal3VO
														.getCellAlpha());
												unitCellBeta = (newCrystal3VO.getCellBeta() == null ? 0 : newCrystal3VO.getCellBeta());
												unitCellGamma = (newCrystal3VO.getCellGamma() == null ? 0 : newCrystal3VO
														.getCellGamma());
											}
										}
									}
									// crystal.setResolution(preObsResolution);
									// Create the crystal name from the uuid and ligandid
									String crystalName = crystal.getCrystalUUID();
									crystal.setName(crystalName);
									crystal.setCellA(unitCellA);
									crystal.setCellB(unitCellB);
									crystal.setCellC(unitCellC);
									crystal.setCellAlpha(unitCellAlpha);
									crystal.setCellBeta(unitCellBeta);
									crystal.setCellGamma(unitCellGamma);
									// crystal = crystalService.create(crystal);
									Crystal3VO crystalC = getCrystal(listCrystalCreated, crystal);
									if (crystalC == null) {
										crystal = crystalService.create(crystal);
										listCrystalCreated.add(crystal);

									} else {
										crystal = crystalC;
									}
									if (!crystal.hasCellInfo()) {
										msgWarning += "Warning: the unit cell parameters are not filled for the spaceGroup "
												+ crystal.getSpaceGroup() + " (" + proteinAcronym + ")!";
									}
									// And add the crystal to the list
									// addCrystal(crystal);
									// Holder
									BLSample3VO holder = new BLSample3VO();
									holder.setCrystalVO(crystal);
									holder.setName(sampleName);
									holder.setCode(pinBarCode);
									holder.setLocation(samplePos);

									// ASSUMPTION: holder is SPINE standard!
									holder.setHolderLength(holderLength);
									holder.setLoopLength(0.5);
									holder.setLoopType(loopType);
									holder.setWireWidth(0.010);
									holder.setComments(sampleComments);
									// Add holder to the container...
									holder.setContainerVO(container);

									holder = sampleService.create(holder);
									// container.addSampleVO(holder);

									holder.setDiffractionPlanVO(difPlan);
									holder = sampleService.update(holder);
								} // end validName
							} // end protein
						}// end crystalID
					} // end sampleRowOK
				} // for dataRow
			} // end dewar != null
				// all samples were empty
			if (sheetIsEmpty) {
				if (container != null) {
					// remove the container
					containerService.deleteByPk(container.getContainerId());
				}
				// TODO understand the following and remove it
				// remove the dewar if no containers
				// Dewar3Service dewarService = (Dewar3Service) ejb3ServiceLocator.getLocalService(Dewar3Service.class);
				// boolean removedOK = true;
				// if (dewar != null) {
				// Dewar3VO existingDewar = dewarService.findByPk(dewar.getDewarId(), false, false);
				// if (existingDewarList == null || existingDewarList.isEmpty()) { // Dewar did not exist
				// removedOK = false;
				// } else {// Dewar was there, deleting it ...
				// dewar = dewarService.findByPk(dewar.getDewarId(), true, true);
				// if (dewar.getContainerVOs().size() == 0) {
				// dewars.remove(dewar);
				// }
				// }
				// }
			}
		}
		String[] msg = new String[2];
		msg[0] = msgError;
		msg[1] = msgWarning;
		return msg;
	}

	/**
	 * Reset dewar and sample counts
	 * 
	 * @param shippingId
	 */
	public static void resetCounts(int shippingId) {
		try {
			// Shipping3Service shippingService = (Shipping3Service) ejb3ServiceLocator
			// .getLocalService(Shipping3Service.class);
			//
			// Shipping3VO shipping = shippingService.findByPk(shippingId, false);
			// shipping.setParcelsNumber(0);

			// Ejb3ServiceLocator ejb3ServiceLocator = Ejb3ServiceLocator.getInstance();
			// Shipping3Service shipping3Service = (Shipping3Service)
			// ejb3ServiceLocator.getLocalService(Shipping3Service.class);
			// Shipping3VO shipping = shipping3Service.findByPk(shippingId, false);
			// shipping.setSamplesNumber(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set dewar and sample count
	 * 
	 * @param shippingId
	 */
	public static void setCounts(int shippingId) {
		try {
			// Shipping3Service shippingService = (Shipping3Service) ejb3ServiceLocator
			// .getLocalService(Shipping3Service.class);

			int nbDewars = 0;
			int nbSamples = 0;

			Ejb3ServiceLocator ejb3ServiceLocator = Ejb3ServiceLocator.getInstance();
			Container3Service container3Service = (Container3Service) ejb3ServiceLocator.getLocalService(Container3Service.class);
			BLSample3Service blSample3Service = (BLSample3Service) ejb3ServiceLocator.getLocalService(BLSample3Service.class);

			// Facades
			// old ContainerFacadeLocal _containers = ContainerFacadeUtil.getLocalHome().create();
			// BlsampleFacadeLocal _samples = BlsampleFacadeUtil.getLocalHome().create();

			// Dewars
			Dewar3Service dewarService = (Dewar3Service) ejb3ServiceLocator.getLocalService(Dewar3Service.class);
			List<Dewar3VO> dewarList = dewarService.findByShippingId(shippingId);
			for (Dewar3VO dewar : dewarList) {
				nbDewars++;
				// Containers
				container3Service.findByDewarId(dewar.getDewarId());
				// old Collection<ContainerLightValue> containerList = _containers.findByDewarId(dewar.getDewarId());
				List<Container3VO> containerList = container3Service.findByDewarId(dewar.getDewarId());
				for (Container3VO container : containerList) {
					// Samples
					// old Collection<BlsampleLightValue> BlsampleList =
					// _samples.findByContainerId(container.getContainerId());
					List<BLSample3VO> BlsampleList = blSample3Service.findByContainerId(container.getContainerId());
					nbSamples += BlsampleList.size();
				}
			}
			// Shipping3VO shipping = shippingService.findByPk(shippingId, false);
			// shipping.setParcelsNumber(nbDewars);
			// shipping.setSamplesNumber(nbSamples);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts from Excel Cell contents to a String
	 * 
	 * @param cell
	 *            The Cell to convert
	 * @return A String value of the contents of the cell
	 */
	public static String cellToString(HSSFCell cell) {
		String retVal = "";
		if (cell == null) {
			return retVal;
		}
		if (cell.getCellType() == HSSFCell.CELL_TYPE_STRING) {
			retVal = cell.getStringCellValue();
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
			retVal = String.valueOf(new Double(cell.getNumericCellValue()).intValue());
		} else if (cell.getCellType() == HSSFCell.CELL_TYPE_BOOLEAN) {
			if (new Boolean(cell.getBooleanCellValue()) == Boolean.TRUE) {
				retVal = "true";
			} else {
				retVal = "false";
			}
		}
		return retVal;
	}

	/**
	 * Converts from Excel Cell contents to a double
	 * 
	 * @param cell
	 *            The Cell to convert
	 * @return The double value contained within the Cell or 0.0 if the Cell is not the correct type or is undefined
	 */
	public static double cellToDouble(HSSFCell cell) {
		Double retVal = new Double(0.0);
		if (cell == null) {
			return retVal.doubleValue();
		}
		if (cell.getCellType() == HSSFCell.CELL_TYPE_NUMERIC) {
			retVal = new Double(cell.getNumericCellValue());
		}
		return retVal.doubleValue();
	}

	private static Crystal3VO getCrystal(List<Crystal3VO> listCrystal, Crystal3VO crystalVO) {
		return CreateShippingFileAction.getCrystal(listCrystal, crystalVO);
	}

}
