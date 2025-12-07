/**
 * Fonctionnalités avancées pour le plugin OBS
 *
 * Ce module fournit des fonctionnalités avancées pour contrôler OBS Studio :
 * - Gestion des sources (visibilité, verrouillage, position, taille, rotation, z-index)
 * - Gestion des filtres (ajout, suppression, paramètres)
 * - Gestion des transitions
 * - Studio Mode (preview, program, transition)
 */

/**
 * Gestion avancée des sources OBS
 */
export class OBSSourceManager {
  constructor(obsClient) {
    this.obsClient = obsClient
  }

  /**
   * Liste toutes les sources d'une scène
   */
  async listSources(sceneName) {
    return await this.obsClient.call('GetSceneItemList', { sceneName })
  }

  /**
   * Obtient les informations d'une source
   */
  async getSourceInfo(sceneName, sourceName) {
    const sources = await this.listSources(sceneName)
    const source = sources.sceneItems.find((item) => item.sourceName === sourceName)
    if (!source) {
      throw new Error(`Source not found: ${sourceName}`)
    }
    return source
  }

  /**
   * Change la visibilité d'une source
   */
  async setSourceVisibility(sceneName, sourceName, visible) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemEnabled', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemEnabled: visible,
    })
  }

  /**
   * Verrouille/déverrouille une source
   */
  async setSourceLocked(sceneName, sourceName, locked) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemLocked', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemLocked: locked,
    })
  }

  /**
   * Change l'ordre des sources (z-index)
   */
  async setSourceIndex(sceneName, sourceName, newIndex) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemIndex', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemIndex: newIndex,
    })
  }

  /**
   * Déplace une source
   */
  async setSourcePosition(sceneName, sourceName, x, y) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemTransform', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemTransform: {
        positionX: x,
        positionY: y,
      },
    })
  }

  /**
   * Redimensionne une source
   */
  async setSourceSize(sceneName, sourceName, width, height) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemTransform', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemTransform: {
        width,
        height,
      },
    })
  }

  /**
   * Fait pivoter une source
   */
  async setSourceRotation(sceneName, sourceName, rotation) {
    const sourceInfo = await this.getSourceInfo(sceneName, sourceName)
    return await this.obsClient.call('SetSceneItemTransform', {
      sceneName,
      sceneItemId: sourceInfo.sceneItemId,
      sceneItemTransform: {
        rotation,
      },
    })
  }
}

/**
 * Gestion des filtres OBS
 */
export class OBSFilterManager {
  constructor(obsClient) {
    this.obsClient = obsClient
  }

  /**
   * Liste les filtres d'une source
   */
  async listFilters(sourceName, sourceType = 'OBS_SOURCE_TYPE_INPUT') {
    return await this.obsClient.call('GetSourceFilterList', {
      sourceName,
      sourceType,
    })
  }

  /**
   * Ajoute un filtre à une source
   */
  async addFilter(sourceName, filterName, filterType, filterSettings = {}) {
    return await this.obsClient.call('CreateSourceFilter', {
      sourceName,
      filterName,
      filterType,
      filterSettings,
    })
  }

  /**
   * Supprime un filtre d'une source
   */
  async removeFilter(sourceName, filterName) {
    return await this.obsClient.call('RemoveSourceFilter', {
      sourceName,
      filterName,
    })
  }

  /**
   * Obtient les paramètres d'un filtre
   */
  async getFilterSettings(sourceName, filterName) {
    return await this.obsClient.call('GetSourceFilter', {
      sourceName,
      filterName,
    })
  }

  /**
   * Met à jour les paramètres d'un filtre
   */
  async setFilterSettings(sourceName, filterName, filterSettings) {
    return await this.obsClient.call('SetSourceFilterSettings', {
      sourceName,
      filterName,
      filterSettings,
    })
  }

  /**
   * Active/désactive un filtre
   */
  async setFilterEnabled(sourceName, filterName, enabled) {
    return await this.obsClient.call('SetSourceFilterEnabled', {
      sourceName,
      filterName,
      filterEnabled: enabled,
    })
  }
}

/**
 * Gestion des transitions OBS
 */
export class OBSTransitionManager {
  constructor(obsClient) {
    this.obsClient = obsClient
  }

  /**
   * Liste toutes les transitions disponibles
   */
  async listTransitions() {
    return await this.obsClient.call('GetTransitionList')
  }

  /**
   * Obtient la transition actuelle
   */
  async getCurrentTransition() {
    return await this.obsClient.call('GetCurrentSceneTransition')
  }

  /**
   * Change la transition
   */
  async setTransition(transitionName, transitionDuration = null) {
    const params = { transitionName }
    if (transitionDuration !== null) {
      params.transitionDuration = transitionDuration
    }
    return await this.obsClient.call('SetCurrentSceneTransition', params)
  }

  /**
   * Déclenche une transition manuelle
   */
  async triggerTransition(transitionName = null, transitionDuration = null) {
    if (transitionName) {
      await this.setTransition(transitionName, transitionDuration)
    }
    return await this.obsClient.call('TriggerStudioModeTransition')
  }
}

/**
 * Gestion du Studio Mode OBS
 */
export class OBSStudioModeManager {
  constructor(obsClient) {
    this.obsClient = obsClient
  }

  /**
   * Active/désactive le Studio Mode
   */
  async setStudioModeEnabled(enabled) {
    return await this.obsClient.call('SetStudioModeEnabled', { studioModeEnabled: enabled })
  }

  /**
   * Obtient l'état du Studio Mode
   */
  async getStudioModeEnabled() {
    return await this.obsClient.call('GetStudioModeEnabled')
  }

  /**
   * Obtient la scène de preview
   */
  async getPreviewScene() {
    return await this.obsClient.call('GetPreviewScene')
  }

  /**
   * Définit la scène de preview
   */
  async setPreviewScene(sceneName) {
    return await this.obsClient.call('SetCurrentPreviewScene', { sceneName })
  }

  /**
   * Obtient la scène de program
   */
  async getProgramScene() {
    return await this.obsClient.call('GetCurrentProgramScene')
  }

  /**
   * Déclenche la transition du Studio Mode
   */
  async triggerTransition(transitionName = null, transitionDuration = null) {
    if (transitionName) {
      const transitionManager = new OBSTransitionManager(this.obsClient)
      await transitionManager.setTransition(transitionName, transitionDuration)
    }
    return await this.obsClient.call('TriggerStudioModeTransition')
  }
}

/**
 * Gestionnaire OBS avancé
 */
export class OBSAdvancedManager {
  constructor(obsClient) {
    this.obsClient = obsClient
    this.sources = new OBSSourceManager(obsClient)
    this.filters = new OBSFilterManager(obsClient)
    this.transitions = new OBSTransitionManager(obsClient)
    this.studioMode = new OBSStudioModeManager(obsClient)
  }

  /**
   * Gère les actions avancées OBS
   */
  async handleAdvancedAction(action, payload) {
    switch (action) {
      // Gestion des sources
      case 'list_sources':
        return await this.sources.listSources(payload.sceneName)
      case 'get_source_info':
        return await this.sources.getSourceInfo(payload.sceneName, payload.sourceName)
      case 'set_source_visibility':
        return await this.sources.setSourceVisibility(
          payload.sceneName,
          payload.sourceName,
          payload.visible
        )
      case 'set_source_locked':
        return await this.sources.setSourceLocked(
          payload.sceneName,
          payload.sourceName,
          payload.locked
        )
      case 'set_source_index':
        return await this.sources.setSourceIndex(
          payload.sceneName,
          payload.sourceName,
          payload.newIndex
        )
      case 'set_source_position':
        return await this.sources.setSourcePosition(
          payload.sceneName,
          payload.sourceName,
          payload.x,
          payload.y
        )
      case 'set_source_size':
        return await this.sources.setSourceSize(
          payload.sceneName,
          payload.sourceName,
          payload.width,
          payload.height
        )
      case 'set_source_rotation':
        return await this.sources.setSourceRotation(
          payload.sceneName,
          payload.sourceName,
          payload.rotation
        )

      // Gestion des filtres
      case 'list_filters':
        return await this.filters.listFilters(payload.sourceName, payload.sourceType)
      case 'add_filter':
        return await this.filters.addFilter(
          payload.sourceName,
          payload.filterName,
          payload.filterType,
          payload.filterSettings
        )
      case 'remove_filter':
        return await this.filters.removeFilter(payload.sourceName, payload.filterName)
      case 'get_filter_settings':
        return await this.filters.getFilterSettings(payload.sourceName, payload.filterName)
      case 'set_filter_settings':
        return await this.filters.setFilterSettings(
          payload.sourceName,
          payload.filterName,
          payload.filterSettings
        )
      case 'set_filter_enabled':
        return await this.filters.setFilterEnabled(
          payload.sourceName,
          payload.filterName,
          payload.enabled
        )

      // Gestion des transitions
      case 'list_transitions':
        return await this.transitions.listTransitions()
      case 'get_current_transition':
        return await this.transitions.getCurrentTransition()
      case 'set_transition':
        return await this.transitions.setTransition(
          payload.transitionName,
          payload.transitionDuration
        )
      case 'trigger_transition':
        return await this.transitions.triggerTransition(
          payload.transitionName,
          payload.transitionDuration
        )

      // Studio Mode
      case 'set_studio_mode':
        return await this.studioMode.setStudioModeEnabled(payload.enabled)
      case 'get_studio_mode':
        return await this.studioMode.getStudioModeEnabled()
      case 'get_preview_scene':
        return await this.studioMode.getPreviewScene()
      case 'set_preview_scene':
        return await this.studioMode.setPreviewScene(payload.sceneName)
      case 'get_program_scene':
        return await this.studioMode.getProgramScene()
      case 'trigger_studio_transition':
        return await this.studioMode.triggerTransition(
          payload.transitionName,
          payload.transitionDuration
        )

      default:
        throw new Error(`Unknown advanced action: ${action}`)
    }
  }
}

