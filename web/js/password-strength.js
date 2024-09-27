/* 
 * This plugin use jQuery Widget Factory because such approach allows to build complex,
 * stateful plugins based on object-oriented principles.
 * If you prefer a lightweight implementation which not use Widget Factory and not 
 * depend from jQuery UI you can use password_strength_lightweight.js.
 * 
 * Dependencies: 
 * 1. jQuery
 * 2. jQuery UI 
 */


;(function ( $, window, document, undefined ) {
    var upperCase = new RegExp('[A-Z]');
    var lowerCase = new RegExp('[a-z]');
    var numbers   = new RegExp('[0-9]');
    var specials  = new RegExp('[-\'/`~!#*$@_%+=.,^&(){}[\]|;:”<>?\\]');

    $.widget( "namespace.strength_meter" , {

        //Options to be used as defaults
        options: {
            strengthWrapperClass: 'strength_wrapper',
            inputClass: 'strength_input',
            strengthMeterClass: 'strength_meter',
            toggleButtonClass: 'button_strength',
            showPasswordText: '{$pw.show}',
            hidePasswordText: '{$pw.hide}',
			//pattern: '^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[#+`~\-_=\'@$!.,;:”^\(\)\[\]\|\{\}\\\/%*<>?&])[^\s]{8,}$'
        },

        _create: function () {
            var options = this.options;
            var val = document.getElementById("password").getAttribute("data-val");
            if (val == null || val == 'null')
            	val = '';
            document.getElementById("password").removeAttribute("data-val");
            
            //Note. Instead of this you can use templating. I did not want to have addition dependencies.
            this.element.addClass(options.strengthWrapperClass);
            this.element.append('<input type="password" value="'+val+'" name="password" class="' + options.inputClass + '">');
            this.element.append('<input type="text" value="'+val+'" name="password" class="' + options.inputClass + '" style="display:none" >');
            this.element.append('<a href="" class="' + options.toggleButtonClass + '">' + options.showPasswordText + '</a>');
            this.element.append('<div class="' + options.strengthMeterClass + '"><div><p></p></div></div>');
            this.element.append(
               '<div class="pswd_info" style="display: none;">'
                +'<h3 class="pw_info">{$pw.info}</h3>'
                +'<ul>'
                  +'<li data-criterion="length" class="valid">8-24 {$pw.chars}</li>'
                  +'<li data-criterion="capital" class="valid">{$pw.capital}</li>'
                  +'<li data-criterion="number" class="valid">{$pw.number}</li>'
                  +'<li data-criterion="special" class="valid">{$pw.special}</li>'
                  +'<li data-criterion="letter" class="valid">{$pw.letter}</li>'
                +'</ul>'
                +'</div>');

            //this object contain all main inner elements which will be used in strength meter.
            this.content = {};

            this.content.$textInput = this.element.find('input[type="text"]');
            this.content.$passwordInput = this.element.find('input[type="password"]');
            this.content.$toggleButton = this.element.find('a');
            this.content.$pswdInfo = this.element.find('.pswd_info');
            this.content.$strengthMeter = this.element.find("." + options.strengthMeterClass);
            this.content.$dataMeter = this.content.$strengthMeter.find("div");
            
            this._sync_inputs(this.content.$passwordInput, this.content.$textInput);
            this._sync_inputs(this.content.$textInput, this.content.$passwordInput);

            this._bind_input_events(this.content.$passwordInput);
            this._bind_input_events(this.content.$textInput);

            var that = this;
            this.content.$toggleButton.bind("click", function(e){
                e.preventDefault();

                that._toggle_input(that.content.$textInput);
                that._toggle_input(that.content.$passwordInput);

                var text = that.content.$passwordInput.is(":visible") ? that.options.showPasswordText: that.options.hidePasswordText;
                $(event.target).text(text);
            });
        },

        //Toggle active inputs.
        _toggle_input: function($element){
            $element.toggle();

            if($element.is(":visible")){
                $element.focus();
            }
        },

        //Copy value from active input inside hidden.
        _sync_inputs: function($s, $t){
            $s.bind('keyup', function () {
                var password = $s.val();
                $t.val(password);
            });
        },

        _bind_input_events: function($s) {
            var that = this;
            $s.bind('keyup', function () {
                var password = $s.val();

                //var characters = (password.length >= 8);
                var capitalletters = password.match(upperCase) ? 1 : 0;
                //var loweletters = password.match(lowerCase) ? 1 : 0;
                var number = password.match(numbers) ? 1 : 0;
                var special = password.match(specials) ? 1 : 0;
                var containWhiteSpace = password.indexOf(' ') >= 0 ? 1 : 0;

				// Unused atm
                //var total = characters + capitalletters + loweletters + number + special;
                //that._update_indicator(total);

                that._update_info('length', password.length >= 8 && password.length <= 24);
                that._update_info('capital', capitalletters);
                that._update_info('number', number);
                that._update_info('special', special);
                that._update_info('letter', !containWhiteSpace);
            }).focus(function () {
                that.content.$pswdInfo.show();
            }).blur(function () {
                that.content.$pswdInfo.hide();
            });
        },

        _update_indicator: function(total) {
            var meter = this.content.$dataMeter;

            meter.removeClass();
            if (total === 0) {
                meter.html('');
            } else if (total === 1) {
                meter.addClass('veryweak').html('<p>very weak</p>');
            } else if (total === 2) {
                meter.addClass('weak').html('<p>weak</p>');
            } else if (total === 3) {
                meter.addClass('medium').html('<p>medium</p>');
            } else {
                meter.addClass('strong').html('<p>strong</p>');
            }
        },

        _update_info: function(criterion, isValid) {
            var $passwordCriteria = this.element.find('li[data-criterion="' + criterion + '"]');

            if (isValid) {
                $passwordCriteria.removeClass('invalid').addClass('valid');
            } else {
                $passwordCriteria.removeClass('valid').addClass('invalid');
            }
        },

        // Destroy an instantiated plugin and clean up 
        // modifications the widget has made to the DOM
        _destroy: function () {
            this.element
            .removeClass( this.options.strengthWrapperClass )
            .text( "" );
        },

        // Respond to any changes the user makes to the 
        // option method
        _setOption: function ( key, value ) {
            switch (key) {
            case "someValue":
                //this.options.someValue = doSomethingWith( value );
                break;
            default:
                //this.options[ key ] = value;
                break;
            }
        }
    });

})( jQuery, window, document );
